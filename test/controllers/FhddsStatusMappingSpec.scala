/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import play.api.libs.json._
import uk.gov.hmrc.fhregistration.services.FhddsApplicationControllerMock
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.test.UnitSpec

class FhddsStatusMappingSpec extends UnitSpec with BaseController {

  val fhddsApplicationController = FhddsApplicationControllerMock.fhddsApplicationController

  "mdtpSubscriptionStatus" should {
    "map MDTP subscription status to received from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))

      val fakeDesResponse: HttpResponse = HttpResponse(200, Some(json))

      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(fakeDesResponse)

      mdtpStatus shouldBe Ok("Received")

    }

    "map MDTP subscription status to Processing from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Sent To DS")))


      val fakeDesResponse: HttpResponse = HttpResponse(200, Some(json))

      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(fakeDesResponse)

      mdtpStatus shouldBe Ok("Processing")

    }

    "map MDTP subscription status to successful from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("successful")))


      val fakeDesResponse: HttpResponse = HttpResponse(200, Some(json))

      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(fakeDesResponse)

      mdtpStatus shouldBe Ok("successful")

    }

    "map MDTP subscription status to Rejected from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Rejected")))


      val fakeDesResponse: HttpResponse = HttpResponse(200, Some(json))

      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(fakeDesResponse)

      mdtpStatus shouldBe Ok("Rejected")

    }

    "map MDTP subscription status to UNEXPECTED_ERROR from DES status" in {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("UNEXPECTED_ERROR")))


      val fakeDesResponse: HttpResponse = HttpResponse(200, Some(json))

      val mdtpStatus = fhddsApplicationController.mdtpSubscriptionStatus(fakeDesResponse)

      mdtpStatus shouldBe Unauthorized("Unexpected business error received.")

    }
  }

}
