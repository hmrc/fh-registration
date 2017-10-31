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

import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhdds.models.des._
import cats.implicits._
import generated.{AddressUKPlusQuestion, PanelNino, PrincipalPlaceOfBusiness, RepeatingCompanyOfficial}
import play.api.libs.json.JsString
import uk.gov.hmrc.fhdds.models.des.IncorporationDetails.dateTimeFormatter

import scala.xml.XML

trait FhddsApplicationService {

  val businessInformationService: BusinessExtraDataService
  val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def iformXmlToApplication(xml: generated.Data, businessRegistrationDetails: BusinessRegistrationDetails): SubScriptionCreate = {

    val businessAddress = businessRegistrationDetails.businessAddress
    val agentReferenceNumber = businessRegistrationDetails.agentReferenceNumber
    val businessName = businessRegistrationDetails.businessName
    val businessType = businessRegistrationDetails.businessType
    val sapNumber = businessRegistrationDetails.sapNumber
    val safeId = businessRegistrationDetails.safeId
    val isAGroup = businessRegistrationDetails.isAGroup
    val directMatch = businessRegistrationDetails.directMatch
    val firstName = businessRegistrationDetails.firstName
    val lastName = businessRegistrationDetails.lastName
    val utr = businessRegistrationDetails.utr
    val identification = businessRegistrationDetails.identification
    val isBusinessDetailsEditable = businessRegistrationDetails.isBusinessDetailsEditable


    val xmlBusinessDetails = xml.businessDetails

    val IntendedStartTradingDate = xmlBusinessDetails.panelIntendedStartTradingDate
    val hasTradingName = xmlBusinessDetails.hasTradingName
    val hasVatRegistrationNumber = xmlBusinessDetails.hasVatRegistrationNumber
    val isNewFulfilmentBusiness = xmlBusinessDetails.isNewFulfilmentBusiness
    val vatRegistaionNumber = xmlBusinessDetails.panelVatRegistrationNumber
    val tradingName = xmlBusinessDetails.panelTradingName


    val businessActivities = xml.businessActivities

    val numberOfCustomersOutsideOfEU = businessActivities.numberOfCustomersOutsideOfEU


    val companyOfficials: Seq[RepeatingCompanyOfficial] = xml.companyOfficials.repeatingCompanyOfficial
    val companyOfficialsDetails = companyOfficials.map(
      companyOfficial ⇒ CompanyOfficials(role = companyOfficial.role,
        name = Names(firstName = companyOfficial.firstName,
          middleName = None,
          lastName = companyOfficial.lastName),
        identification = Identification(nino = Some(companyOfficial.panelNino.getOrElse(PanelNino("")).nino))
      )
    )

    val contactPerson = xml.contactPerson

    val contactPersonAddress = contactPerson.addressLookUpContactAddress.flatMap(
      address ⇒ address.blockAddressInternationalPlus.map(
        blockAddressInternationalPlus ⇒ Address(
          blockAddressInternationalPlus.line1,
          Some(blockAddressInternationalPlus.line2),
          blockAddressInternationalPlus.line3,
          Some(""),
          "",
          blockAddressInternationalPlus.country_code.getOrElse("GB")
        )
      )
    )

    val otherStorageSites = xml.otherStorageSites

    val otherStorageSitesDetail = otherStorageSites.panelOtherStorageSites.map(
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


    val principalPlaceOfBusinessDetails = xml.principalPlaceOfBusiness

    val principalPlaceOfBusinessDetailsAddress =
      principalPlaceOfBusinessDetails.panelPreviousPrincipalTradingBusinessAddresses.map(
        previousPrincipalTradingBusinessAddresses ⇒
          previousPrincipalTradingBusinessAddresses.repeatingPreviousPrincipalTradingBusinessAddress.blockAddressUKPlus.map(
            blockAddressUKPlus ⇒ Address(
              blockAddressUKPlus.line1,
              blockAddressUKPlus.line2,
              blockAddressUKPlus.line3,
              blockAddressUKPlus.town,
              blockAddressUKPlus.postcode,
              "GB"
            )
          ).toList
      )

    val businessAddressForFHDDS = Address(businessAddress.line1,
      Some(businessAddress.line2),
      businessAddress.line3,
      businessAddress.line4,
      businessAddress.postcode.getOrElse(""),
      businessAddress.country)

    SubScriptionCreate(
      FhddsApplication(
        organizationType = "Limited Liability Partnership",
        businessDetail = BusinessDetail(
          LimitedLiabilityPartnershipCorporateBody(
            IncorporationDetails(
              companyRegistrationNumber = "AB123456",
              dateOfIncorporation = LocalDate.now()
            )
          )
        ),
        businessAddressForFHDDS = BusinessAddressForFHDDS(
          currentAddress = businessAddressForFHDDS,
          commonDetails = CommonDetails(
            telephone = Some(contactPerson.telephoneNumber),
            mobileNumber = None,
            email = "email@email.com"
          ),
          dateStartedTradingAsFulfilmentHouse = LocalDate.now(),
          isOnlyPrinicipalPlaceOfBusinessInLastThreeYears = principalPlaceOfBusinessDetails.isPrincipalPlaceOfBusinessForLastThreeYears.contains("true"),
          previousOperationalAddress = principalPlaceOfBusinessDetailsAddress
        ),
        contactDetail = ContactDetail(
          title = None,
          names = Names(
            firstName = contactPerson.firstName,
            lastName = contactPerson.lastName
          ),
          usingSameContactAddress = true,
          address = contactPersonAddress,
          commonDetails = CommonDetails(
            telephone = Some(contactPerson.telephoneNumber),
            mobileNumber = None,
            email = "email@email.com"
          ),
          roleInOrganization = Some(RoleInOrganization())
        ),
        additionalBusinessInformation = AdditionalBusinessInformationwithType(
          partnerCorporateBody = Some(
            PartnerCorporateBody(
              numberOfOtherOfficials = companyOfficials.size,
              companyOfficials = Some(companyOfficialsDetails.toList)
            )
          ),
          allOtherInformation = AllOtherInformation(
            fulfilmentOrdersType = OnLineOnly(),
            numberOfCustomers = numberOfCustomersOutsideOfEU,
            premises = Premises(numberOfpremises = otherStorageSitesDetail.getOrElse(List(businessAddressForFHDDS)).length.toString,
              address = otherStorageSitesDetail.getOrElse(List(businessAddressForFHDDS)))
          )
        ),
        declaration = Declaration(personName = s"$firstName $lastName",
          personStatus = "",
          personStatusOther = Some(""),
          isInformationAccurate = true),
        FHbusinessDetail = FHbusinessDetail(isNewFulfilmentBusiness = isNewFulfilmentBusiness.toBoolean))
    )
  }


  def getPreviousPrincipalTradingBusinessAddress(ppob: PrincipalPlaceOfBusiness): Option[List[Address]] = {
    //    if (isYes(ppob.isPrincipalPlaceOfBusinessForLastThreeYears))
    //      None
    //    else {
    //      for {
    //        panelPpob ← ppob.panelPreviousPrincipalTradingBusinessAddresses
    //        ukPanel ← panelPpob.repeatingPreviousPrincipalTradingBusinessAddress.
    //        blockAddressUk ← ukPanel.block_addressUKPlus
    //      } yield {
    //        ukAddressToAddress(blockAddressUk)
    //      }
    //    }
    ???
  }

  def ukAddressToAddress(blockAddressUk: AddressUKPlusQuestion) =
    Address(
      blockAddressUk.line1,
      blockAddressUk.line2,
      blockAddressUk.line3,
      blockAddressUk.town,
      blockAddressUk.postcode,
      "GB")

  def mkRegisteredAdderess(brd: BusinessRegistrationDetails): Address = ???

  def isYes(radioButtonAnswer: String): Boolean = radioButtonAnswer equals "Yes"


  def businessRegistrationDetails(submissionRef: String): Either[String, BusinessRegistrationDetails] = ???

  //    Either fromOption (
  //      businessInformationService getBusinessRegistrationDetails submissionRef,
  //      "business registration details not found"
  //    )

}
