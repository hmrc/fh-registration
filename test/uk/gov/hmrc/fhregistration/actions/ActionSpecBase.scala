/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.util.Timeout
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.mvc.{ActionFunction, ActionRefiner, Results}
import uk.gov.hmrc.fhregistration.util.UnitSpec

import scala.concurrent.{Await, Promise}

trait ActionSpecBase
    extends UnitSpec with ScalaFutures with MockitoSugar with BeforeAndAfterEach with Matchers with Results
    with Status {

  def refinedRequest[P[_], R[_], A](action: ActionRefiner[R, P], request: R[A])(implicit timeout: Timeout) = {
    val p = Promise[P[_]]
    val result = action.invokeBlock(request, { r: P[A] =>
      p success r
      Ok
    })

    status(result) shouldBe OK
    Await.result(p.future, timeout.duration)
  }

  def result[P[_], R[_], A](action: ActionFunction[R, P], request: R[A]) =
    action.invokeBlock(request, { r: P[A] =>
      Ok
    })

}
