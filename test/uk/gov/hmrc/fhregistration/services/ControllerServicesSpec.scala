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

package uk.gov.hmrc.fhregistration.services

import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Success
import scala.util.matching.Regex
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class ControllerServicesSpec extends UnitSpec {

  val fhddsApplicationController = FhddsApplicationControllerMock.fhddsApplicationController
  implicit val request = FhddsApplicationControllerMock.request
  implicit val hc = FhddsApplicationControllerMock.headerCarrier

  when(FhddsApplicationControllerMock.mockEmailConnectorImplConnector.sendEmail(any(), any())(any(), any(),any()))
    .thenReturn(Future successful null)

  "createSubmissionRef" should {
    "Generates a submission reference number" in {

      val theDigitsSubmissionRefRegex: Regex = "([A-Z0-9]{3})-([A-Z,-9]{4})-([A-Z0-9]{3})".r

      val submissionRef = ControllerServices.createSubmissionRef()

      theDigitsSubmissionRefRegex.unapplySeq(submissionRef).isDefined shouldBe true

    }
  }

  "emailConnector" should {
    "send email if there is an email from the user" in {
      val result = fhddsApplicationController.sendEmail("test@email.com","testSubmissionRef")
      result.value.get shouldBe Success(null)
    }
    "no email will send if there is not an email from the user" in {
      val result = fhddsApplicationController.sendEmail("","testSubmissionRef")
      result.value.get shouldBe Success(null)
    }
  }
}
