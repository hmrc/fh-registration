/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.fhregistration.models.des.DesStatus.DesStatus
import uk.gov.hmrc.fhregistration.models.des.StatusResponse

class StatusResponseSpec extends AnyWordSpec with Matchers {

  "StatusResponse" should {

    "serialize to JSON" in {
      val response = StatusResponse(
        subscriptionStatus = DesStatus.Successful,
        idType = Some("UTR"),
        idValue = Some("1234567890")
      )
      val json = Json.toJson(response)

      (json \ "subscriptionStatus").as[DesStatus] shouldEqual DesStatus.Successful
      (json \ "idType").asOpt[String] shouldEqual Some("UTR")
      (json \ "idValue").asOpt[String] shouldEqual Some("1234567890")
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "subscriptionStatus" -> "Successful",
        "idType"             -> "UTR",
        "idValue"            -> "1234567890"
      )
      val result = json.validate[StatusResponse]

      result shouldEqual JsSuccess(
        StatusResponse(
          subscriptionStatus = DesStatus.Successful,
          idType = Some("UTR"),
          idValue = Some("1234567890")
        )
      )
    }

    "handle missing optional fields during deserialization" in {
      val json = Json.obj(
        "subscriptionStatus" -> "Successful"
      )
      val result = json.validate[StatusResponse]

      result shouldEqual JsSuccess(
        StatusResponse(
          subscriptionStatus = DesStatus.Successful,
          idType = None,
          idValue = None
        )
      )
    }
  }
}
