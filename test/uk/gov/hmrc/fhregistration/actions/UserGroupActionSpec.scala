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

package uk.gov.hmrc.fhregistration.actions

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.mvc.ControllerComponents
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}

import scala.concurrent.Future
import play.api.test.Helpers._

class UserGroupActionSpec extends ActionSpecBase {

  val mockAuthConnector = mock[AuthConnector]
  val controllerComponents: ControllerComponents = Helpers.stubControllerComponents()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }

  val request = FakeRequest()
  val action = new UserGroupAction(mockAuthConnector, controllerComponents)

  "refine" should {
    "Fail with UNAUTHORIZED" in {
      setupAuthConnector(new Exception())
      status(result(action, request)) shouldBe UNAUTHORIZED
    }
  }

  // TODO: Reduce repetition.
  def setupAuthConnector(throwable: Throwable) =
    when(
      mockAuthConnector.authorise(any(), any[Retrieval[Option[String] ~ Enrolments]])(any(), any())
    ) thenReturn Future
      .failed(throwable)
}
