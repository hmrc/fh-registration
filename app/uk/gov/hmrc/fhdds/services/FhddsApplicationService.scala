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
import uk.gov.hmrc.fhdds.models.des.{Address, FhddsApplication}
import cats.implicits._
import generated.{AddressUKPlusQuestion, PrincipalPlaceOfBusiness}

import scala.xml.XML

trait FhddsApplicationService {

  val businessInformationService: BusinessExtraDataService
  val dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def iformXmlToApplication(submissionRef: String, xmlString: String): Either[String, FhddsApplication] = {
    for {
      brd ← businessRegistrationDetails(submissionRef)
      xml = XML loadString xmlString
      data ← scalaxb.fromXMLEither[generated.Data](xml)
    } yield {

      FhddsApplication(
        isNewFulfilmentBusiness = isYes(data.businessDetails.isNewFulfilmentBusiness),
        intendedStartTradingDate =
          data
            .businessDetails
            .panelIntendedStartTradingDate
            .map(_.intendedStartTradingDate)
            .map(LocalDate.parse(_, dtf)),
        companyName = brd.businessName, //TODO
        companyUTR = brd.utr.get,//TODO not get
        hasTradingName = isYes(data.businessDetails.hasTradingName),
        tradingName = data.businessDetails.panelTradingName.map(_.tradingName),
        hasVatRegistrationNumber = isYes(data.businessDetails.hasVatRegistrationNumber),
        vatRegistrationNumber = data.businessDetails.panelVatRegistrationNumber.map(_.vatRegistrationNumber),
        companyRegistrationNumber = ???,
        numberOfCustomersOutsideOfEU = data.businessActivities.numberOfCustomersOutsideOfEU,
        isPrincipalPlaceOfBusinessForLastThreeYears = isYes(data.principalPlaceOfBusiness.isPrincipalPlaceOfBusinessForLastThreeYears),
        numberOfYearsAtPrincipalBusinessAddress = data
          .principalPlaceOfBusiness
          .panelNumberOfYearsAtPrincipalBusinessAddress
          .map(_.numberOfYearsAtPrincipalBusinessAddress),

        registeredAddress = mkRegisteredAdderess(brd),
        principalTradingBusinessAddress = mkRegisteredAdderess(brd),
        previousPrincipalTradingBusinessAddresses = ???,
        hasOtherStorageSites = isYes(data.otherStorageSites.hasOtherStorageSites),
        otherStorageSites = ???,
        contactAddress = ???,
        companyOfficials = ???,
        contactPerson = ???

//        previousPrincipalTradingBusinessAddress = getPreviousPrincipalTradingBusinessAddress(xml.principalPlaceOfBusiness),
//        principalTradingBusinessAddress = registeredAddress,
//        otherStorageSites = getOtherStorageSites(xml.otherBusinessPremises),
//        contactAddress = contactAddress,
//
//        intendedStartTradingDate = getIntendedStartTradingDate(xml.businessDetails),
//        companyUTR = ???,
//        tradingName = ???,
//        vatRegistrationNumber = ???,
//        companyRegistrationNumber = ???,
//        numberOfCustomersOutsideOfEU = ???,
//        companyOfficials = ???,
//        isPrincipalPlaceOfBusinessForLastThreeYears = ???,
//        numberOfYearAtPrincipalBusinessAddress = ???,
//        contactPerson = ???

      )

    }
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
