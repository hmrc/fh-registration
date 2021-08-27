/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.mvc.ControllerComponents
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.fhregistration.testsupport.UserTestData

import scala.concurrent.Future

class UserActionSpec extends ActionSpecBase {

  val mockAuthConnector = mock[AuthConnector]
  val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  val request = FakeRequest()
  val action = UserAction(mockAuthConnector, controllerComponents)

  "refine" should {
    "Fail with BAD_REQUEST when the internal user id can no be found" in {
      setupAuthConnector()
      status(result(action, request)) shouldBe BAD_REQUEST
    }

    "Fail with UNAUTHORIZED" in {
      setupAuthConnector(MissingBearerToken())
      status(result(action, request)) shouldBe UNAUTHORIZED
    }

    "Fail with BAD_GATEWAY" in {
      setupAuthConnector(new Exception())
      status(result(action, request)) shouldBe BAD_GATEWAY
    }

    "Return a user request with correct user id and no registration number" in {
      setupAuthConnector(Some(UserTestData.testUserId))

      val userRequest = refinedRequest(action, request)
      userRequest.userId shouldBe UserTestData.testUserId
      userRequest.registrationNumber shouldBe None
    }

    "Return a user request with correct user id and registration number" in {
      val fhddsEnrolment = EnrolmentIdentifier("EtmpRegistrationNumber", "XZFH00000123456")
      val otherEnrolment = EnrolmentIdentifier("EtmpRegistrationNumber", "XZSDIL000123456")

      val enrolments = Set(
        new Enrolment("HMRC-OBTDS-ORG", Seq(otherEnrolment), "Active"),
        new Enrolment("HMRC-OBTDS-ORG", Seq(fhddsEnrolment), "Active")
      )

      setupAuthConnector(Some(UserTestData.testUserId), enrolments)

      val userRequest = refinedRequest(action, request)
      userRequest.userId shouldBe UserTestData.testUserId
      userRequest.registrationNumber shouldBe Some(fhddsEnrolment.value)
    }
  }

  def setupAuthConnector(internalId: Option[String] = None, enrolments: Set[Enrolment] = Set.empty) = {
    val authResult = Future successful (new ~(internalId, new Enrolments(enrolments)))
    when(mockAuthConnector.authorise(any(), any[Retrieval[Option[String] ~ Enrolments]])(any(), any())) thenReturn authResult
  }

  def setupAuthConnector(throwable: Throwable) =
    when(mockAuthConnector.authorise(any(), any[Retrieval[Option[String] ~ Enrolments]])(any(), any())) thenReturn Future
      .failed(throwable)
}
