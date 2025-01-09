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

package uk.gov.hmrc.fhregistration.models.businessregistration

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.fhregistration.models.TaxEnrolmentsCallback

class TaxEnrolmentsCallbackSpec extends PlaySpec {

  "TaxEnrolmentsCallback" should {

    "correctly determine if succeeded is true" in {
      val callback = TaxEnrolmentsCallback(url = "http://test.url", state = "SUCCEEDED", errorResponse = None)
      callback.succeeded mustBe true
    }

    "correctly determine if succeeded is false for non-SUCCEEDED state" in {
      val callbackFailed =
        TaxEnrolmentsCallback(url = "http://test.url", state = "FAILED", errorResponse = Some("Some error"))
      callbackFailed.succeeded mustBe false

      val callbackPending = TaxEnrolmentsCallback(url = "http://test.url", state = "PENDING", errorResponse = None)
      callbackPending.succeeded mustBe false
    }

    "serialize to JSON correctly" in {
      val callback = TaxEnrolmentsCallback(url = "http://test.url", state = "SUCCEEDED", errorResponse = None)
      val json: JsValue = Json.toJson(callback)

      (json \ "url").as[String] mustBe "http://test.url"
      (json \ "state").as[String] mustBe "SUCCEEDED"
      (json \ "errorResponse").asOpt[String] mustBe None
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse("""
        {
          "url": "http://test.url",
          "state": "SUCCEEDED",
          "errorResponse": null
        }
      """)
      val callback = json.as[TaxEnrolmentsCallback]

      callback.url mustBe "http://test.url"
      callback.state mustBe "SUCCEEDED"
      callback.errorResponse mustBe None
    }

    "handle optional errorResponse during deserialization" in {
      val jsonWithError: JsValue = Json.parse("""
        {
          "url": "http://test.url",
          "state": "FAILED",
          "errorResponse": "Some error occurred"
        }
      """)
      val callbackWithError = jsonWithError.as[TaxEnrolmentsCallback]

      callbackWithError.url mustBe "http://test.url"
      callbackWithError.state mustBe "FAILED"
      callbackWithError.errorResponse mustBe Some("Some error occurred")
    }

    "serialize and deserialize correctly (round-trip)" in {
      val callback = TaxEnrolmentsCallback(url = "http://test.url", state = "SUCCEEDED", errorResponse = None)
      val json: JsValue = Json.toJson(callback)
      val deserializedCallback = json.as[TaxEnrolmentsCallback]

      deserializedCallback mustBe callback
    }
  }
}
