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

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsValue, Json}

class AddressSpec extends AnyFunSuite {

  val address1 = Address(
    line1 = "Test Line 1",
    line2 = "Test Line 2",
    line3 = Some("Test Line 3"),
    line4 = Some("Test Line 4"),
    postcode = Some("Test Code"),
    country = "Test country"
  )

  val address2 = Address(
    line1 = "Test Line 1",
    line2 = "Test Line 2",
    line3 = None,
    line4 = None,
    postcode = None,
    country = "Test country"
  )

  test("Address should serialize and deserialize correctly") {

    val addressJson: JsValue = Json.toJson(address1)
    val expectedJson: JsValue = Json.obj(
      "line1"    -> "Test Line 1",
      "line2"    -> "Test Line 2",
      "line3"    -> "Test Line 3",
      "line4"    -> "Test Line 4",
      "postcode" -> "Test Code",
      "country"  -> "Test country"
    )

    assert(addressJson == expectedJson)
    val deserializedAddress: Address = addressJson.as[Address]
    assert(deserializedAddress == address1)
  }

  test("Address should serialize and deserialize correctly when optional fields are missing") {

    val addressJson: JsValue = Json.toJson(address2)
    val expectedJson: JsValue = Json.obj(
      "line1"   -> "Test Line 1",
      "line2"   -> "Test Line 2",
      "country" -> "Test country"
    )

    assert(addressJson == expectedJson)
    val deserializedAddress: Address = addressJson.as[Address]
    assert(deserializedAddress == address2)
  }

  test("Address should return correct string representation") {

    val expectedString = "Test Line 1, Test Line 2, Test Line 3, Test Line 4, Test Code, Test country"
    assert(address1.toString == expectedString)
  }

  test("Address should return correct string representation when optional fields are missing") {

    val expectedString = "Test Line 1, Test Line 2, Test country"
    assert(address2.toString == expectedString)
  }
}
