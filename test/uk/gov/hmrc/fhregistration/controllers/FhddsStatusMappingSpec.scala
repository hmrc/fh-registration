/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.controllers

import play.api.libs.json._
import uk.gov.hmrc.fhregistration.models.des.StatusResponse
import uk.gov.hmrc.fhregistration.models.fhdds.FhddsStatus
import uk.gov.hmrc.fhregistration.testsupport.mocks.FhddsMocks
import uk.gov.hmrc.fhregistration.util.UnitSpec

class FhddsStatusMappingSpec extends UnitSpec with FhddsMocks {

  val fhddsApplicationController = fhddsApplicationControllerWithMocks

  "mdtpSubscriptionStatus" should {
    "map MDTP subscription status to received from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Received
    }

    "map MDTP subscription status to Processing from DES status for Sent To DS" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Sent To DS")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Processing
    }

    "map MDTP subscription status to Processing from DES status for In processing" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("In processing")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Processing
    }

    "map MDTP subscription status to Processing from DES status for DS Outcome In Progress" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("DS Outcome In Progress")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Processing
    }


    "map MDTP subscription status to Processing from DES status for Sent to RCM" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Sent to RCM")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Processing
    }

    "map MDTP subscription status to Successful from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Successful")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Approved
    }

    "map MDTP subscription status to Rejected from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Rejected")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Rejected
    }

    "map MDTP subscription status to ApprovedWithConditions from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Approved with Conditions")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.ApprovedWithConditions
    }

    "map MDTP subscription status to Revoked from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Revoked")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Revoked
    }

    "map MDTP subscription status to Withdrawal from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Withdrawal")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Withdrawn
    }

    "map MDTP subscription status to Deregistered from DES status" in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("De-Registered")))
      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Deregistered
    }

    "throw exception in case of unexpected status " in {
      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Contract Object Inactive")))

      an[IllegalArgumentException] should be thrownBy fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)
    }
  }

}
