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

package uk.gov.hmrc.fhregistration.actions

import org.mockito.Mockito.{reset, when}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{AuthConnector, MissingBearerToken}
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import play.api.test.Helpers._
import uk.gov.hmrc.fhregistration.testsupport.UserTestData

import scala.concurrent.Future

class UserActionSpec extends ActionSpecBase {

  val mockAuthConnector = mock[AuthConnector]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }


  val request = FakeRequest()
  val action = new UserAction(mockAuthConnector)

  "refine" should {
    "Fail with BAD_REQUEST when the internal user id can no be found" in {
      setupAuthConnector()
      status(result(action, request)) shouldBe BAD_REQUEST
    }

    "Fail with UNAUTHORIZED" in {
      setupAuthConnector(MissingBearerToken())
      status(result(action, request)) shouldBe UNAUTHORIZED
    }

    "Return a user request with correct user id" in {
      setupAuthConnector(Some(UserTestData.testUserId))

      val userRequest = refinedRequest(action, request)
      userRequest.userId shouldBe UserTestData.testUserId
    }

  }

  def setupAuthConnector(internalId: Option[String] = None) = {
    when(mockAuthConnector.authorise(any(),any[Retrieval[Option[String]]])(any(), any())) thenReturn Future.successful(internalId)
  }

  def setupAuthConnector(throwable: Throwable) = {
    when(mockAuthConnector.authorise(any(),any[Retrieval[Option[String]]])(any(), any())) thenReturn Future.failed(throwable)
  }

}