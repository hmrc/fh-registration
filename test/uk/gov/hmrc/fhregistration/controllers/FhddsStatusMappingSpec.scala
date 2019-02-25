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
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.test.UnitSpec

class FhddsStatusMappingSpec extends UnitSpec with BaseController with FhddsMocks {

  val fhddsApplicationController = fhddsApplicationControllerWithMocks

  "mdtpSubscriptionStatus" should {
    "map MDTP subscription status to received from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))

      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

      mdtpStatus shouldBe FhddsStatus.Received

    }

    "map MDTP subscription status to Processing from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Sent To DS")))



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

    "throw exception in case of unexpected status " in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Contract Object Inactive")))

      an[IllegalArgumentException] should be thrownBy fhddsApplicationController.mdtpSubscriptionStatus(json.as[StatusResponse].subscriptionStatus)

    }
  }

}
