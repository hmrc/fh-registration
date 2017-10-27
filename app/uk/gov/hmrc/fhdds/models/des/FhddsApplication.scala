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

package uk.gov.hmrc.fhdds.models.des

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json.{JsString, Json, Reads, Writes}


case class FhddsApplication(organizationType: String = "Limited Liability Partnership",
                            companyRegistrationNumber: String = "AB123456",
                            dateOfIncorporation: LocalDate = LocalDate.now(),
                            businessAddressForFHDDS: BusinessAddressForFHDDS,
                            contactDetail: ContactDetail,
                            additionalBusinessInformation: AdditionalBusinessInformation,
                            declaration:Declaration
//                            isNewFulfilmentBusiness: Boolean,
//                            intendedStartTradingDate: Option[LocalDate],
//                            limitedLiabilityOrCorporateBodyWithGroup: Option[LimitedLiabilityOrCorporateBodyWithGroup],
//                            companyName: String,
//                            companyUTR: String,
//                            hasTradingName: Boolean,
//                            tradingName: Option[String],
//                            hasVatRegistrationNumber: Boolean,
//                            vatRegistrationNumber: Option[String],
//                            numberOfCustomersOutsideOfEU: String,
//                            isPrincipalPlaceOfBusinessForLastThreeYears: Boolean,
//                            numberOfYearsAtPrincipalBusinessAddress: Option[String],
//                            registeredAddress: Address,
//                            principalTradingBusinessAddress: Address,
//                            previousPrincipalTradingBusinessAddresses: Option[List[Address]],
//                            hasOtherStorageSites: Boolean,
//                            otherStorageSites: Option[List[Address]],
//                            contactAddress: AnyAddress,
//                            companyOfficials: List[CompanyOfficial],
//                            contactPerson: ContactPerson
                           )

case class Declaration(personName: String,
                       personStatus:String,
                       isInformationAccurate: Boolean)

object Declaration {
  implicit val format = Json.format[Declaration]
}


case class Email(email: String)

object Email {
  implicit val format = Json.format[Email]
}

case class ContactDetail(firstName: String,
                         lastName: String,
                         usingSameContactAddress: Boolean = true,
                         commonDetails: Email)

object ContactDetail {
  implicit val format = Json.format[ContactDetail]
}

//case class GroupMemberDetail(numberOfMembersInGroup: String, memberDetails: List[MemberDetails])
//
//object GroupMemberDetail {
//  implicit val format = Json.format[GroupMemberDetail]
//}

//case class LimitedLiabilityOrCorporateBodyWithGroup(creatingFHDDSGroup: Boolean,
//                                                    confirmationByRepresentative: Boolean,
//                                                    GroupMemberDetail: GroupMemberDetail)
//
//object LimitedLiabilityOrCorporateBodyWithGroup {
//  implicit val format = Json.format[LimitedLiabilityOrCorporateBodyWithGroup]
//}

case class Name(companyName: String, tradingName: String)

object Name {
  implicit val format = Json.format[Name]
}

case class MemberDetails(name: Name, tradingName: String)

object MemberDetails {
  implicit val format = Json.format[MemberDetails]
}

case class Premises(numberOfPremises: String,
                    thirdPartyStorageUsed:Boolean,
                    goodsImportedOutEORI: Boolean)

object Premises {
  implicit val format = Json.format[Premises]
}


case class AdditionalBusinessInformation(fulfilmentOrdersType: String,
                                         numberOfCustomers:String,
                                         premises:Premises,
                                         address:Address)
object AdditionalBusinessInformation {
  implicit val format = Json.format[AdditionalBusinessInformation]
}

case class BusinessAddressForFHDDS(currentAddress: Address,
                                   commontDetails: Email = Email(""),
                                   dateStartedTradingAsFulfilmentHouse: LocalDate = LocalDate.now(),
                                   isOnlyPrinicipalPlaceOfBusinessInLastThreeYears: Boolean)
object BusinessAddressForFHDDS {
  val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  implicit val localDateReads = Reads.localDateReads("dd/MM/yyyy")
  implicit val localDateWrites = Writes { date: LocalDate ⇒
    JsString(date.format(dateTimeFormatter))
  }

  implicit val format = Json.format[BusinessAddressForFHDDS]

}


object FhddsApplication {
  val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  implicit val localDateReads = Reads.localDateReads("dd/MM/yyyy")
  implicit val localDateWrites = Writes { date: LocalDate ⇒
    JsString(date.format(dateTimeFormatter))
  }

  implicit val format = Json.format[FhddsApplication]

}
