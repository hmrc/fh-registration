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

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsValue, Json}

class IdentificationSpec extends AnyFunSuite {

  test("Identification should serialize and deserialize correctly") {
    val identification = Identification(
      idNumber = "123456789",
      issuingInstitution = "HMRC",
      issuingCountryCode = "GB"
    )

    val identificationJson: JsValue = Json.toJson(identification)
    val expectedJson: JsValue = Json.obj(
      "id_number"            -> "123456789",
      "issuing_institution"  -> "HMRC",
      "issuing_country_code" -> "GB"
    )

    assert(identificationJson == expectedJson)
    val deserializedIdentification: Identification = identificationJson.as[Identification]
    assert(deserializedIdentification == identification)
  }
}
