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
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class BusinessRegistrationDetailsSpec extends AnyFunSuite with Matchers {

  val address: Address = Address(
    line1 = "Test Line 1",
    line2 = "Test Line 2",
    line3 = Some("Test Line 3"),
    line4 = Some("Test Line 4"),
    postcode = Some("Test Code"),
    country = "Test country"
  )

  val identification: Identification = Identification(
    idNumber = "test id",
    issuingInstitution = "test institution",
    issuingCountryCode = "test country"
  )

  test("BusinessRegistrationDetails should serialize and deserialize correctly to/from JSON") {
    val businessDetails = BusinessRegistrationDetails(
      businessName = "test Business",
      businessType = Some("test type"),
      businessAddress = address,
      sapNumber = "test number",
      safeId = "test safe id",
      agentReferenceNumber = Some("number"),
      firstName = Some("firstname"),
      lastName = Some("lastname"),
      utr = Some("utr number"),
      identification = Some(identification),
      isAGroup = true,
      directMatch = true,
      isBusinessDetailsEditable = true
    )

    val json = Json.toJson(businessDetails)
    val deserializedBusinessDetails = json.as[BusinessRegistrationDetails]

    deserializedBusinessDetails shouldBe businessDetails
  }
}
