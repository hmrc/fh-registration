/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.fhdds.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import generated.{AddressInternationalPlusQuestion, AddressLookUpContactAddress, AddressUKPlusQuestion, Data, PanelNino, PrincipalPlaceOfBusiness, RepeatingCompanyOfficial}
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhdds.models.des._

trait FhddsApplicationService {

  val DefaultOrganizationType = "Corporate Body"
  val DefaultCompanyRegistrationNumber = "AB123456"
  val DefaultIncorporationDate = LocalDate.of(2010, 1, 1)
  val DefaultContactEmail = "email@email.com"

  val DefaultFirstName = "John"
  val DefaultLastName = "Doe"

  val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def iformXmlToApplication(xml: generated.Data, brd: BusinessRegistrationDetails): SubScriptionCreate = {
    SubScriptionCreate(
      FhddsApplication(
        organizationType = brd.businessType.getOrElse(DefaultOrganizationType),
        businessDetail = businessDetails(xml, brd),
        businessAddressForFHDDS = businessAddressForFHDDS(xml, brd),
        contactDetail = contactDetail(xml, brd),
        additionalBusinessInformation = additionalBusinessInformation(brd, xml),
        declaration = declaration(xml),
        FHbusinessDetail = FHbusinessDetail(isNewFulfilmentBusiness = isYes(xml.businessDetails.isNewFulfilmentBusiness)))
    )
  }

  private def declaration(xml: Data) = {
    val firstName = DefaultFirstName
    val lastName = DefaultLastName
    Declaration(personName = s"$firstName $lastName",
      personStatus = "",
      personStatusOther = Some(""),
      isInformationAccurate = true)
  }

  private def additionalBusinessInformation(brd: BusinessRegistrationDetails, xml: Data) = {
    val numberOfCustomersOutsideOfEU = xml.businessActivities.numberOfCustomersOutsideOfEU


    val officials = companyOfficialsDetails(xml)

    val otherStorageSitesDetail = otherStorageSitesDetails(xml)

    AdditionalBusinessInformationwithType(
      partnerCorporateBody = Some(
        PartnerCorporateBody(
          numberOfOtherOfficials = officials.size.toString,
          companyOfficials = Some(officials)
        )
      ),
      allOtherInformation = AllOtherInformation(
        fulfilmentOrdersType = OnLineOnly(),
        numberOfCustomers = numberOfCustomersOutsideOfEU,
        premises = Premises(numberOfpremises = otherStorageSitesDetail.getOrElse(List(principalBusinessAddress(brd))).length.toString,
          address = otherStorageSitesDetail.getOrElse(List(principalBusinessAddress(brd))))
      )
    )
  }

  private def otherStorageSitesDetails(xml: Data) = {
    xml.otherStorageSites.panelOtherStorageSites.map(
      otherStorageSites ⇒
        otherStorageSites.repeatingSectionOtherTradingPremises.flatMap(
          blockAddressUKPlus ⇒
            blockAddressUKPlus.repeatingPanel.flatMap(
              repeatingPanel ⇒ repeatingPanel.otherTradingPremisesAddressLookup.ukPanel.flatMap(
                ukPanel ⇒ ukPanel.blockAddressUKPlus.map(
                  blockAddressUKPlus ⇒ Address(
                    blockAddressUKPlus.line1,
                    blockAddressUKPlus.line2,
                    blockAddressUKPlus.line3,
                    blockAddressUKPlus.town,
                    blockAddressUKPlus.postcode,
                    "GB"
                  )
                )
              )
            )
        ).toList
    )
  }

  def companyOfficialsDetails(xml: generated.Data) = {
    val companyOfficials: Seq[RepeatingCompanyOfficial] = xml.companyOfficials.repeatingCompanyOfficial
    companyOfficials.toList.map(
      companyOfficial ⇒ CompanyOfficials(role = {
        companyOfficial.role match {
          case role if (role.contains("Director") || role.contains("Secretary")) ⇒ "Director and Company Secretary"
          case role if (role.contains("Director")) ⇒ "Director"
          case role if (role.contains("Director")) ⇒ "Company Secretary"
          case _ ⇒ "Member"
        }
      },
        name = Names(firstName = companyOfficial.firstName,
          middleName = None,
          lastName = companyOfficial.lastName),
        identification = Identification(nino = Some(companyOfficial.panelNino.getOrElse(PanelNino("")).nino))
      )
    )
  }

  private def contactDetail(xml: Data, brd: BusinessRegistrationDetails) = {
    val contactPersonAddress = extractContactPersonAddress(xml, brd)

    ContactDetail(
      title = None,
      names = Names(
        firstName = xml.contactPerson.firstName,
        lastName = xml.contactPerson.lastName),
      usingSameContactAddress = true,
      address = contactPersonAddress,
      commonDetails = CommonDetails(
        telephone = Some(xml.contactPerson.telephoneNumber),
        mobileNumber = None,
        email = DefaultContactEmail),
      roleInOrganization = Some(RoleInOrganization())
    )
  }

  private def extractContactPersonAddress(xml: Data, brd: BusinessRegistrationDetails): Option[Address] = {
    if (isYes(xml.contactPerson.contactCorrectAddress)) {
      Some(principalBusinessAddress(brd))
    } else {
      xml.contactPerson.addressLookUpContactAddress.flatMap(addressLookupToAddress)
    }
  }

  def addressLookupToAddress(addressLookup: AddressLookUpContactAddress): Option[Address] = {
    addressLookup.selectLocation.map(isYes).flatMap {
      case true ⇒ addressLookup.ukPanel.flatMap( _.blockAddressUKPlus).map(ukAddressToAddress)
      case false ⇒ addressLookup.blockAddressInternationalPlus.map(internationalAddressToAddress)
    }
  }

  def businessDetails(xml: generated.Data, brd: BusinessRegistrationDetails) = {
    BusinessDetail(
      LimitedLiabilityPartnershipCorporateBody(
        IncorporationDetails(
          companyRegistrationNumber = DefaultCompanyRegistrationNumber,
          dateOfIncorporation = DefaultIncorporationDate
        )
      )
    )
  }

  def businessAddressForFHDDS(xml: generated.Data, brd: BusinessRegistrationDetails) = {
    val isOnlyPrinicipalPlaceOfBusinessInLastThreeYears = isYes(xml.principalPlaceOfBusiness.isPrincipalPlaceOfBusinessForLastThreeYears)
    BusinessAddressForFHDDS(
      currentAddress = principalBusinessAddress(brd),
      commonDetails = CommonDetails(
        telephone = Some(xml.contactPerson.telephoneNumber),
        mobileNumber = None,
        email = DefaultContactEmail
      ),
      dateStartedTradingAsFulfilmentHouse = LocalDate.now(),
      isOnlyPrinicipalPlaceOfBusinessInLastThreeYears = isOnlyPrinicipalPlaceOfBusinessInLastThreeYears,
      previousOperationalAddress = {
        if (isOnlyPrinicipalPlaceOfBusinessInLastThreeYears) None
        else previousPrincipalPlaceOfBusinessAddresses(xml)
      }
    )
  }

  def previousPrincipalPlaceOfBusinessAddresses(xml: Data): Option[List[PreviousOperationalAddress]] = {
    val principalPlaceOfBusiness = xml.principalPlaceOfBusiness
    Some(List(
      PreviousOperationalAddress(
        operatingDate = LocalDate.of(2010, 1, 1),
        previousAddress = {
          for {
            previousPrincipalTradingBusinessAddresses ← principalPlaceOfBusiness.panelPreviousPrincipalTradingBusinessAddresses
            blockAddressUKPlus ← previousPrincipalTradingBusinessAddresses.repeatingPreviousPrincipalTradingBusinessAddress.blockAddressUKPlus
          } yield {
            Address(
              blockAddressUKPlus.line1,
              blockAddressUKPlus.line2,
              blockAddressUKPlus.line3,
              blockAddressUKPlus.town,
              blockAddressUKPlus.postcode,
              "GB"
            )
          }
        }.get
      )
    ))
  }

  def principalBusinessAddress(brd: BusinessRegistrationDetails) = {
    Address(brd.businessAddress.line1,
      Some(brd.businessAddress.line2),
      brd.businessAddress.line3,
      brd.businessAddress.line4,
      brd.businessAddress.postcode.getOrElse(""),
      brd.businessAddress.country)
  }


  def ukAddressToAddress(blockAddressUk: AddressUKPlusQuestion) =
    Address(
      blockAddressUk.line1,
      blockAddressUk.line2,
      blockAddressUk.line3,
      blockAddressUk.town,
      blockAddressUk.postcode,
      "GB")

  def internationalAddressToAddress(blockAddressInternationalPlus: AddressInternationalPlusQuestion) =
    Address(
      blockAddressInternationalPlus.line1,
      Some(blockAddressInternationalPlus.line2),
      blockAddressInternationalPlus.line3,
      Some(""),
      "",
      blockAddressInternationalPlus.country_code.getOrElse("GB"))

  def isYes(radioButtonAnswer: String): Boolean = radioButtonAnswer equals "Yes"



}
