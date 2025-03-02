/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.models.businessregistration

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Format, Json, JsonConfiguration}

case class BusinessRegistrationDetails(
  businessName: String,
  businessType: Option[String],
  businessAddress: Address,
  sapNumber: String,
  safeId: String,
  isAGroup: Boolean = false,
  directMatch: Boolean = false,
  agentReferenceNumber: Option[String],
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  utr: Option[String] = None,
  identification: Option[Identification] = None,
  isBusinessDetailsEditable: Boolean = false
)

object BusinessRegistrationDetails {
  implicit val formats: Format[BusinessRegistrationDetails] = {
    implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
    Json.format[BusinessRegistrationDetails]
  }
}
