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

package uk.gov.hmrc.fhregistration.models.des

import play.api.libs.json.Json

case class SubscriptionCreateRequestSchema(
  organizationType: String,
  FHbusinessDetail: IsNewFulfilmentBusiness,
  GroupInformation: Option[LimitedLiabilityOrCorporateBodyWithOutGroup],
  additionalBusinessInformation: AdditionalBusinessInformationwithType,
  businessDetail: BusinessDetail,
  businessAddressForFHDDS: BusinessAddressForFHDDS,
  contactDetail: ContactDetail,
  declaration: Declaration)

object SubscriptionCreateRequestSchema {
  implicit val format = Json.format[SubscriptionCreateRequestSchema]
}

case class SubScriptionCreate(subScriptionCreate: SubscriptionCreateRequestSchema)


object SubScriptionCreate {
  implicit val format = Json.format[SubScriptionCreate]
}

