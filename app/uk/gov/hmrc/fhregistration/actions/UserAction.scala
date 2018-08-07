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
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{allEnrolments, internalId}
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future

class UserRequest[A](val userId: String, val registrationNumber: Option[String], request: Request[A])
  extends WrappedRequest(request) {
}

class UserAction @Inject()(val authConnector: AuthConnector)
  extends ActionBuilder[UserRequest]
    with ActionRefiner[Request, UserRequest]
    with AuthorisedFunctions
    with MicroserviceAction
{

  val serviceName = "HMRC-OBTDS-ORG"
  val identifierName = "ETMPREGISTRATIONNUMBER"

  override protected def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = {
    implicit val r = request

    authorised().retrieve(internalId and allEnrolments) {
      case Some(id) ~ enrolments ⇒
        Future successful Right(new UserRequest(id, registrationNumber(enrolments), request))
      case _     ⇒
        Future successful error(BadRequest, "Can not find user id")

    } recover {
      case e: AuthorisationException ⇒
        Logger.warn(s"Unauthorized user: $e")
        error(Unauthorized, s"Unauthorized: ${e.getMessage}")
      case e: Throwable ⇒
        Logger.warn(s"Bad gateway while authorizing user: $e")
        error(BadGateway, s"Bad gateway: ${e.getMessage}")
    }
  }

  private def registrationNumber(enrolments: Enrolments): Option[String] = {
    val fhddsRegistrationNumbers = for {
      enrolment ← enrolments.enrolments
      if enrolment.key equalsIgnoreCase serviceName

      identifier ← enrolment.identifiers
      if identifier.key equalsIgnoreCase identifierName
      if identifier.value.slice(2, 4) == "FH"

    } yield {
      identifier.value
    }

    fhddsRegistrationNumbers.headOption

  }
}
