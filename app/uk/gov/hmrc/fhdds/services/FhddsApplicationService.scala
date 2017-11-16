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
import javax.inject.Singleton

import com.google.inject.ImplementedBy
import generated.{AddressInternationalPlusQuestion, AddressLookUpContactAddress, AddressUKPlusQuestion, Data, RepeatingCompanyOfficial}
import org.apache.commons.lang3.text.WordUtils
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhdds.models.des._

@Singleton
class FhddsApplicationServiceImpl extends FhddsApplicationService

@ImplementedBy(classOf[FhddsApplicationServiceImpl])
trait FhddsApplicationService {

  val DefaultOrganizationType = "Corporate Body"
  val DefaultCompanyRegistrationNumber = "AB123456"
  val DefaultIncorporationDate: LocalDate = LocalDate.of(2010, 1, 1)
  val DefaultContactEmail = "email@email.com"
  val DefaultPersonDeclarationStatus = "Director"
  val DefaultNumberOfCustomers = "01"

  val DefaultFirstName = "John"
  val DefaultLastName = "Doe"

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def iformXmlToApplication(xml: generated.Data, brd: BusinessRegistrationDetails): SubScriptionCreate = {
    SubScriptionCreate( SubscriptionCreateRequestSchema(
        organizationType = brd.businessType.map(translateBusinessType).getOrElse(DefaultOrganizationType),
        FHbusinessDetail = IsNewFulfilmentBusiness(isNewFulfilmentBusiness = isYes(xml.businessDetails.isNewFulfilmentBusiness),
                                                   proposedStartDate =  getProposedStartDate(xml)),
        GroupInformation = Some(LimitedLiabilityOrCorporateBodyWithOutGroup(creatingFHDDSGroup = true,
                                                                            confirmationByRepresentative = true,
                                                                            groupMemberDetail = None)),
        additionalBusinessInformation = additionalBusinessInformation(brd, xml),
        businessDetail = businessDetails(xml, brd),
        businessAddressForFHDDS = businessAddressForFHDDS(xml, brd),
        contactDetail = contactDetail(xml, brd),
        declaration = declaration(xml)
    ))
  }

  private def translateBusinessType(businessType: String) = WordUtils.capitalizeFully(businessType) match {
    case "Sole Trader" ⇒ "Sole Proprietor"
    case other         ⇒ other
  }

  private def getProposedStartDate(xml: Data) = {
    for {
      panelProposedStartDate <- xml.businessDetails.panelProposedStartDate
    } yield {
      LocalDate.parse(panelProposedStartDate.proposedStartDate, dtf)
    }
  }

  private def declaration(xml: Data) = {
    Declaration(personName = xml.declaration.personName,
      personStatus = xml.declaration.personStatus,
      personStatusOther = None,
      isInformationAccurate = true)
  }

  private def additionalBusinessInformation(brd: BusinessRegistrationDetails, xml: Data) = {
    //val numberOfCustomersOutsideOfEU = xml.businessActivities.numberOfCustomersOutsideOfEU
    val numberOfCustomers = DefaultNumberOfCustomers//TODO
    val officials = companyOfficialsDetails(xml)

    val otherStorageSitesDetail = {
      if (isYes(xml.otherStorageSites.hasOtherStorageSites)) {
        otherStorageSitesDetails(xml)
      } else None
    }

    AdditionalBusinessInformationwithType(
      partnerCorporateBody = Some(
        PartnerCorporateBody(
          numberOfOtherOfficials = officials.size.toString,
          companyOfficials = Some(officials)
        )
      ),
      allOtherInformation = AllOtherInformation(
        fulfilmentOrdersType = FulfilmentOrdersType(typeOfOtherOrder= None),
        numberOfCustomers = numberOfCustomers,
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
                    blockAddressUKPlus.line2.getOrElse(" "),
                    blockAddressUKPlus.line3,
                    blockAddressUKPlus.town,
                    Some(blockAddressUKPlus.postcode),
                    "GB"
                  )
                )
              )
            )
        ).toList
    )
  }

  def companyOfficialsDetails(xml: generated.Data): List[CompanyOfficial] = {
    val companyOfficials: Seq[RepeatingCompanyOfficial] = xml.companyOfficials.repeatingCompanyOfficial
    companyOfficials.toList.map(
      companyOfficial ⇒ CompanyOfficial(role = {
        companyOfficial.role match {
          case "Secretary" ⇒ "Company Secretary"
          case "Director+Secretary" ⇒ "Director and Company Secretary"
          case "Director" ⇒ "Director"
          case _ ⇒ "Member"
        }
      },

        name = {
          for {
            panelPerson ← companyOfficial.panelPerson
          } yield {
            Name(firstName = panelPerson.firstName,
                  middleName = None,
                  lastName = panelPerson.lastName)
          }
        }.get,

        identification = {
          for {
            panelPerson ← companyOfficial.panelPerson
          } yield {
            if (isYes(panelPerson.hasNino)) {
              Identification(nino = panelPerson.panelNino.map(_.nino))
            } else {
              Identification(passportNumber = panelPerson.panelNoNino.flatMap(
                                                personNoNino ⇒ if (isYes(personNoNino.hasPassportNumber)) {
                                                  personNoNino.panelPassportNumber.map(
                                                    passportNumber ⇒ passportNumber.passportNumber
                                                  )
                                                } else None
                                              ),
                             nationalIdNumber = panelPerson.panelNoNino.flatMap(
                                                 personNoNino ⇒
                                                   personNoNino.panelNationalIDNumber.map(_.nationalIdNumber)
                                               )
              )
            }
          }
        }.get

      )
    )
  }

  private def contactDetail(xml: Data, brd: BusinessRegistrationDetails) = {
    val contactPersonAddress = extractContactPersonAddress(xml, brd)

    ContactDetail(
      title = None,
      names = Name(
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
    if (isYes(addressLookup.selectLocation.getOrElse(""))) {
      addressLookup.ukPanel.flatMap(_.blockAddressUKPlus).map(ukAddressToAddress)
    } else {
      addressLookup.blockAddressInternationalPlus.map(internationalAddressToAddress)
    }
  }

  def businessDetails(xml: generated.Data, brd: BusinessRegistrationDetails): BusinessDetail = {
    BusinessDetail(
      LimitedLiabilityPartnershipCorporateBody(
        groupRepresentativeJoinDate = Some(DefaultIncorporationDate),
        IncorporationDetails(
          companyRegistrationNumber = Some(DefaultCompanyRegistrationNumber),
          dateOfIncorporation = Some(DefaultIncorporationDate)
        )
      )
    )
  }

  def businessAddressForFHDDS(xml: generated.Data, brd: BusinessRegistrationDetails): BusinessAddressForFHDDS = {
    val isOnlyPrincipalPlaceOfBusinessInLastThreeYears =
      isYes(xml.principalPlaceOfBusiness match {
        case Some(principalPlaceOfBusiness) ⇒ principalPlaceOfBusiness.isOnlyPrinicipalPlaceOfBusinessInLastThreeYears
        case _ ⇒ "no"
      })
    BusinessAddressForFHDDS(
      currentAddress = principalBusinessAddress(brd),
      commonDetails = CommonDetails(
        telephone = Some(xml.contactPerson.telephoneNumber),
        mobileNumber = None,
        email = xml.contactPerson.email
      ),
      dateStartedTradingAsFulfilmentHouse = xml.principalPlaceOfBusiness match {
        case Some(principalPlaceOfBusiness) ⇒ LocalDate.parse(principalPlaceOfBusiness.dateStartedTradingAsFulfilmentHouse, dtf)
        case _ ⇒ LocalDate.now()
      },
      isOnlyPrinicipalPlaceOfBusinessInLastThreeYears = isOnlyPrincipalPlaceOfBusinessInLastThreeYears,
      previousOperationalAddress = {
        if (isOnlyPrincipalPlaceOfBusinessInLastThreeYears) None
        else previousPrincipalPlaceOfBusinessAddresses(xml)
      }
    )
  }

  def previousPrincipalPlaceOfBusinessAddresses(xml: Data): Option[List[PreviousOperationalAddress]] = {
    val principalPlaceOfBusinessO = xml.principalPlaceOfBusiness
    Some(List(
      PreviousOperationalAddress(
        operatingDate = DefaultIncorporationDate,
        previousAddress = {
          for {
            principalPlaceOfBusiness ← principalPlaceOfBusinessO
            panelPreviousPrincipalTradingBusinessAddresses ← principalPlaceOfBusiness.panelPreviousPrincipalTradingBusinessAddresses
            blockAddressUKPlus ← panelPreviousPrincipalTradingBusinessAddresses.repeatingPreviousPrincipalTradingBusinessAddress.blockAddressUKPlus
          } yield {
            Address(
              blockAddressUKPlus.line1,
              blockAddressUKPlus.line2.getOrElse(" "),
              blockAddressUKPlus.line3,
              blockAddressUKPlus.town,
              Some(blockAddressUKPlus.postcode),
              "GB"
            )
          }
        }.get
      )
    ))
  }

  def principalBusinessAddress(brd: BusinessRegistrationDetails): Address = {
    Address(line1 = brd.businessAddress.line1,
            line2 = checkEmptyAddressLine(brd.businessAddress.line2),
            line3 = brd.businessAddress.line3,
            town = brd.businessAddress.line4,
            postalCode = brd.businessAddress.postcode,
            countryCode = brd.businessAddress.country)
  }

  def ukAddressToAddress(blockAddressUk: AddressUKPlusQuestion) =
    Address(
      blockAddressUk.line1,
      blockAddressUk.line2.getOrElse(" "),
      blockAddressUk.line3,
      blockAddressUk.town,
      Some(blockAddressUk.postcode),
      "GB")

  def internationalAddressToAddress(blockAddressInternationalPlus: AddressInternationalPlusQuestion) =
    Address(
      blockAddressInternationalPlus.line1,
      checkEmptyAddressLine(blockAddressInternationalPlus.line2),
      blockAddressInternationalPlus.line3,
      None,
      None,
      blockAddressInternationalPlus.country_code.getOrElse("GB"))

  def isYes(radioButtonAnswer: String): Boolean = radioButtonAnswer equals "Yes"

  private def checkEmptyAddressLine(addressLine: String): String = {
    if (addressLine.isEmpty) " "
    else addressLine
  }

}