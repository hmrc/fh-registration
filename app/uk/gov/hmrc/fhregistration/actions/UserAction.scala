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

import javax.inject.Inject

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.internalId

import scala.concurrent.Future

class UserRequest[A](val userId: String, request: Request[A])
  extends WrappedRequest(request) {
}

class UserAction @Inject()(val authConnector: AuthConnector)
  extends ActionBuilder[UserRequest]
    with ActionRefiner[Request, UserRequest]
    with AuthorisedFunctions
    with MicroserviceAction
{

  override protected def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = {
    implicit val r = request

    authorised().retrieve(internalId) {
      case Some(id) ⇒
        Future successful Right(new UserRequest(id, request))
      case _     ⇒
        Future successful error(BadRequest, "Can not find user id")

    } recover { case e ⇒
      Logger.warn("Unauthorized user", e)
      error(Unauthorized, s"Unauthorized: ${e.getMessage}")
    }
  }
}
