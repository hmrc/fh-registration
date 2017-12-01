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

import play.api.libs.json._


case class IndividualIdentification(passportNumber: Option[String] = None,
                                    nationalIdNumber: Option[String] = None,
                                    nino: Option[String] = None)

case class CompanyIdentification(vatRegistrationNumber: Option[String] = None,
                                 uniqueTaxpayerReference: Option[String] = None,
                                 companyRegistrationNumber: Option[String] = None)

case class SoleProprietorIdentification(nino: Option[String] = None,
                                        vatRegistrationNumber: Option[String] = None,
                                        uniqueTaxpayerReference: Option[String] = None)

case class NonProprietorIdentification(vatRegistrationNumber: Option[String] = None,
                                       uniqueTaxpayerReference: Option[String] = None)

object IndividualIdentification {
  implicit val format = Json.format[IndividualIdentification]
}

object CompanyIdentification {
  implicit val format = Json.format[CompanyIdentification]
}

object SoleProprietorIdentification {
  implicit val format = Json.format[SoleProprietorIdentification]
}
