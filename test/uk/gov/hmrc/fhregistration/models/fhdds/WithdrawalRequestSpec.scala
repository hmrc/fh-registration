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

package uk.gov.hmrc.fhregistration.models.fhdds

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsSuccess, JsValue, Json}

class WithdrawalRequestSpec extends AnyFunSuite {

  test("WithdrawalRequest should serialize and deserialize correctly") {
    val emailAddress = "test@email.com"
    val withdrawal: JsValue = Json.parse("""{"reason": "User requested withdrawal"}""")
    val withdrawalRequest = WithdrawalRequest(emailAddress, withdrawal)

    val json = Json.toJson(withdrawalRequest)
    assert((json \ "emailAddress").as[String] == emailAddress)
    assert((json \ "withdrawal").as[JsValue] == withdrawal)

    val deserialized = json.validate[WithdrawalRequest]
    assert(deserialized == JsSuccess(withdrawalRequest))
  }
}
