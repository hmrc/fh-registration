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

package uk.gov.hmrc.fhregistration.models.fhdds

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsSuccess, JsValue, Json}

class SubmissionRequestSpec extends AnyFunSuite {

  test("SubmissionRequest should serialize and deserialize correctly") {
    val emailAddress = "test@email.com"
    val submission: JsValue = Json.parse("""{"key": "value"}""")
    val submissionRequest = SubmissionRequest(emailAddress, submission)

    val json = Json.toJson(submissionRequest)
    assert((json \ "emailAddress").as[String] == emailAddress)
    assert((json \ "submission").as[JsValue] == submission)

    val deserialized = json.validate[SubmissionRequest]
    assert(deserialized == JsSuccess(submissionRequest))
  }
}
