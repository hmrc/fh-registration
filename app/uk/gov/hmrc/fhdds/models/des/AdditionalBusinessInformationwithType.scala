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

import play.api.libs.json.Json

case class Identification(passportNumber: Option[String] = Some(""), nationalIdNumber: Option[String] = Some(""), nino: Option[String] = Some(""))

object Identification {
  implicit val format = Json.format[Identification]
}

case class Premises(numberOfpremises: String = "2",
                    address: List[Address])

object Premises {
  implicit val format = Json.format[Premises]
}

trait CompanyOfficialsRole

case class CompanyOfficialsDirector(role: String = "Director") extends CompanyOfficialsRole

case class CompanyOfficialsSecretary(role: String = "Company Secretary") extends CompanyOfficialsRole

object CompanyOfficialsDirector {
  implicit val format = Json.format[CompanyOfficialsDirector]
}

case class CompanyOfficialsDirectorSecretary(role: String = "Director and Company Secretary") extends CompanyOfficialsRole

case class CompanyOfficialsMember(role: String = "Member") extends CompanyOfficialsRole

case class CompanyOfficials(role: String = "Director",
                            name: Names,
                            identification: Identification)

object CompanyOfficials {
  implicit val format = Json.format[CompanyOfficials]
}

case class PartnerCorporateBody(numberOfOtherOfficials: Int = 1,
                                companyOfficials: Option[List[CompanyOfficials]])

object PartnerCorporateBody {
  implicit val format = Json.format[PartnerCorporateBody]
}

case class AllOtherInformation(fulfilmentOrdersType: FulfilmentOrdersType,
                               numberOfCustomers: String,
                               premises: Premises,
                               thirdPartyStorageUsed: Boolean = false,
                               goodsImportedOutEORI: Boolean = true)

object AllOtherInformation {
  implicit val format = Json.format[AllOtherInformation]
}

case class AdditionalBusinessInformationwithType(partnerCorporateBody: Option[PartnerCorporateBody],
                                                 allOtherInformation: AllOtherInformation)

object AdditionalBusinessInformationwithType {
  implicit val format = Json.format[AdditionalBusinessInformationwithType]
}
