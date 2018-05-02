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

import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}

import scala.concurrent.Future
import uk.gov.hmrc.auth.core.retrieve.Retrievals.groupIdentifier
import scala.concurrent.ExecutionContext.Implicits.global

class UserGroupRequest[A](val groupId: String, request: Request[A]) extends WrappedRequest(request)

class UserGroupAction(val authConnector: AuthConnector)
  extends ActionBuilder[UserGroupRequest]
    with ActionRefiner[Request, UserGroupRequest]
    with AuthorisedFunctions
    with MicroserviceAction
{

  override protected def refine[A](request: Request[A]): Future[Either[Result, UserGroupRequest[A]]] = {
    implicit val r = request

    authorised().retrieve(groupIdentifier) {
      case Some(groupId) ⇒
        Future successful Right(new UserGroupRequest[A](groupId, request))
      case _ ⇒
        Logger.error("group id not found")
        Future successful error(BadRequest, "group id not found")
    }

  } recover { case e ⇒
    Logger.warn("Unauthorized user", e)
    error(Unauthorized, s"Unauthorized: ${e.getMessage}")
  }

}
