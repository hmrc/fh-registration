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
import uk.gov.hmrc.fhregistration.models.businessregistration.{Address, BusinessRegistrationDetails, Identification}

class BusinessRegistrationDetailsSpec extends AnyFunSuite {

  import org.scalatest.funsuite.AnyFunSuite
  import play.api.libs.json.{JsValue, Json}

  class BusinessRegistrationDetailsTest extends AnyFunSuite {

    val address = Address(
      line1 = "Test Line 1",
      line2 = "Test Line 2",
      line3 = Some("Test Line 3"),
      line4 = Some("Test Line 4"),
      postcode = Some("Test Code"),
      country = "Test country"
    )

    val identification = Identification(
      idNumber = "test id",
      issuingInstitution = "test institution",
      issuingCountryCode = "test country"
    )

    val businessRegistrationDetails = BusinessRegistrationDetails(
      businessName = "test business",
      businessType = Some("test type"),
      businessAddress = address,
      sapNumber = "test number",
      safeId = "test safe id",
      directMatch = true,
      agentReferenceNumber = Some("number"),
      firstName = Some("firstname"),
      lastName = Some("lastname"),
      utr = Some("utr number"),
      identification = Some(identification),
      isBusinessDetailsEditable = true
    )

    test("BusinessRegistrationDetails should serialize and deserialize correctly") {

      val businessRegistrationDetailsJson: JsValue = Json.toJson(businessRegistrationDetails)
      val expectedJson: JsValue = Json.obj(
        "business_name" -> "test business",
        "business_type" -> "test Company",
        "business_address" -> Json.obj(
          "line1"    -> "Test Line 1",
          "line2"    -> "Test Line 2",
          "line3"    -> "Test Line 3",
          "line4"    -> "Test Line 4",
          "postcode" -> "Test code",
          "country"  -> "Test country"
        ),
        "sap_number"             -> "test number",
        "safe_id"                -> "test safe id",
        "direct_match"           -> true,
        "agent_reference_number" -> "number",
        "firstname"              -> "firstname",
        "lastname"               -> "lastname",
        "utr"                    -> "utr number",
        "identification" -> Json.obj(
          "id_number"            -> "test id",
          "issuing_institution"  -> "test institution",
          "issuing_country_code" -> "test country"
        ),
        "is_business_details_editable" -> true
      )

      assert(businessRegistrationDetailsJson == expectedJson)
      val deserializedBusinessRegistrationDetails: BusinessRegistrationDetails =
        businessRegistrationDetailsJson.as[BusinessRegistrationDetails]
      assert(deserializedBusinessRegistrationDetails == businessRegistrationDetails)
    }

    test("BusinessRegistrationDetails should serialize and deserialize correctly with missing optional fields") {

      val businessRegistrationDetails = BusinessRegistrationDetails(
        businessName = "test business",
        businessType = Some("test type"),
        businessAddress = address,
        sapNumber = "test number",
        safeId = "test safe id",
        isAGroup = true,
        agentReferenceNumber = None,
        firstName = None,
        lastName = None,
        utr = None,
        identification = None
      )

      val businessRegistrationDetailsJson: JsValue = Json.toJson(businessRegistrationDetails)
      val expectedJson: JsValue = Json.obj(
        "business_name" -> "test business",
        "business_type" -> "test type",
        "business_address" -> Json.obj(
          "line1"   -> "Test line 1",
          "line2"   -> "Test line 2",
          "country" -> "Test country"
        ),
        "sap_number"                   -> "test number",
        "safe_id"                      -> "test safe id",
        "is_a_group"                   -> true,
        "direct_match"                 -> false,
        "is_business_details_editable" -> false
      )

      assert(businessRegistrationDetailsJson == expectedJson)
      val deserializedBusinessRegistrationDetails: BusinessRegistrationDetails =
        businessRegistrationDetailsJson.as[BusinessRegistrationDetails]
      assert(deserializedBusinessRegistrationDetails == businessRegistrationDetails)
    }

    test("BusinessRegistrationDetails should handle missing optional fields correctly") {

      val businessRegistrationDetails = BusinessRegistrationDetails(
        businessName = "test business",
        businessType = None,
        businessAddress = address,
        sapNumber = "test number",
        safeId = "test sase id",
        directMatch = true,
        agentReferenceNumber = None,
        firstName = None,
        lastName = None,
        utr = None,
        identification = None
      )

      val businessRegistrationDetailsJson: JsValue = Json.toJson(businessRegistrationDetails)

      val expectedJson: JsValue = Json.obj(
        "business_name" -> "test business",
        "business_address" -> Json.obj(
          "line1"   -> "Test line 1",
          "line2"   -> "Test line 2",
          "country" -> "Test country"
        ),
        "sap_number"                   -> "test number",
        "safe_id"                      -> "test safe id",
        "is_a_group"                   -> false,
        "direct_match"                 -> true,
        "is_business_details_editable" -> false
      )

      assert(businessRegistrationDetailsJson == expectedJson)
      val deserializedBusinessRegistrationDetails: BusinessRegistrationDetails =
        businessRegistrationDetailsJson.as[BusinessRegistrationDetails]
      assert(deserializedBusinessRegistrationDetails == businessRegistrationDetails)
    }
  }
}
