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

package uk.gov.hmrc.fhregistration.models.hip

import play.api.libs.functional.syntax.*
import play.api.libs.json.{Reads, __}
import uk.gov.hmrc.fhregistration.models.des.StatusResponse
import uk.gov.hmrc.fhregistration.models.hip.HipStatus.HipStatus

case class SubscriptionStatusResponse(
  subscriptionStatus: HipStatus,
  idType: Option[String],
  idValue: Option[String]
) {
  def toDesStatusResponse =
    StatusResponse(HipStatus.toDesStatus(subscriptionStatus), idType, idValue)
}

object SubscriptionStatusResponse {
  implicit val reads: Reads[SubscriptionStatusResponse] = (
    (__ \ "success" \ "subscriptionStatus").read[HipStatus] and
      (__ \ "success" \ "idType").readNullable[String] and
      (__ \ "success" \ "idValue").readNullable[String]
  )(SubscriptionStatusResponse.apply)
}
