/*
 * Copyright 2020 HM Revenue & Customs
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
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._

class UserGroupRequest[A](val userId: String, val groupId: String, request: Request[A]) extends WrappedRequest(request)

case class UserGroupAction(val authConnector: AuthConnector, cc: ControllerComponents)
    extends MicroserviceAction()(cc.executionContext) with ActionRefiner[Request, UserGroupRequest]
    with AuthorisedFunctions with ActionBuilder[UserGroupRequest, AnyContent] {

  override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser
  override protected val executionContext: ExecutionContext = cc.executionContext
  override protected def refine[A](request: Request[A]): Future[Either[Result, UserGroupRequest[A]]] = {
    implicit val r = request

    authorised().retrieve(internalId and groupIdentifier) {
      case Some(userId) ~ Some(groupId) ⇒
        Future successful Right(new UserGroupRequest[A](userId, groupId, request))
      case _ ⇒
        Logger.error("group id not found")
        Future successful error(BadRequest, "group id not found")
    }

  } recover {
    case e ⇒
      Logger.warn("Unauthorized user", e)
      error(Unauthorized, s"Unauthorized: ${e.getMessage}")
  }

}
