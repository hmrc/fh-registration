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

package uk.gov.hmrc.fhdds.models

import play.api.libs.json.{Json, OFormat}


case class EtmpAddress(addressLine1: Option[String], addressLine2: Option[String],
  addressLine3                     : Option[String], addressLine4: Option[String],
  postalCode                       : Option[String], countryCode: Option[String])

object EtmpAddress {
  implicit val formats: OFormat[EtmpAddress] = Json.format[EtmpAddress]
}

case class OrganisationResponse(organisationName: String, isAGroup: Option[Boolean], organisationType: Option[String])

object OrganisationResponse {
  implicit val formats: OFormat[OrganisationResponse] = Json.format[OrganisationResponse]
}

case class FindBusinessDataResponse(isAnASAgent: Boolean,
  agentReferenceNumber                         : Option[String],
  sapNumber                                    : Option[String],
  safeId                                       : Option[String],
  address                                      : EtmpAddress,
  organisation                                 : Option[OrganisationResponse] = None)

object FindBusinessDataResponse {
  implicit val format: OFormat[FindBusinessDataResponse] = Json.format[FindBusinessDataResponse]
}

