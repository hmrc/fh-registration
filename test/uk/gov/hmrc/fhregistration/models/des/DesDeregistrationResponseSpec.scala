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
import uk.gov.hmrc.fhregistration.models.des.DesDeregistrationResponse

import java.util.Date

class DesDeregistrationResponseSpec extends AnyWordSpec with Matchers {

  "DesDeregistrationResponse" should {

    "serialize to JSON" in {
      val processingDate = new Date()
      val response = DesDeregistrationResponse(processingDate)
      val json = Json.toJson(response)

      (json \ "processingDate").as[Date] shouldEqual processingDate
    }

    "deserialize from JSON" in {
      val processingDate = new Date()
      val json = Json.obj("processingDate" -> processingDate)
      val result = json.validate[DesDeregistrationResponse]

      result shouldEqual JsSuccess(DesDeregistrationResponse(processingDate))
    }
  }
}
