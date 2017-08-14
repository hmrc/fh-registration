/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.fhdds.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.Retrievals.allEnrolments
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.fhdds.config.MicroserviceAuthConnector
import uk.gov.hmrc.fhdds.services.FhddsPrepopulationDataProvider
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.fhdds.models.FhddsModels.companyDetailsWriter
import uk.gov.hmrc.play.http.HeaderCarrier

object FhddsController extends BaseController with AuthorisedFunctions {
  lazy val authProvider: AuthProviders = AuthProviders(GovernmentGateway)
  override val authConnector: uk.gov.hmrc.auth.core.AuthConnector = MicroserviceAuthConnector

  def hc1(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  def companyDetails() = authorisedUserWithUtr {
    implicit request ⇒ utr ⇒
        FhddsPrepopulationDataProvider
          .getCompanyDetails(utr)
          .map { companyDetails ⇒
            println(s"====$companyDetails")
            Ok(Json.toJson(companyDetails))
          }
  }

  def authorisedUserWithUtr(action: Request[AnyContent] ⇒ String ⇒ Future[Result]): Action[AnyContent] =
    Action.async { implicit request ⇒
      authorised(authProvider).retrieve(allEnrolments) {
        findUtr(ConfidenceLevel.L200, _) match {
          case Some(utr) ⇒ action(request)(utr)
          case None      ⇒ Future successful Unauthorized("Unauthorized")
        }
      } recover {
        case _: Throwable ⇒ Unauthorized("Unauthorized")
      }
    }

  def findUtr(minConfidence: ConfidenceLevel, userEnrolments: Enrolments): Option[String] = {
    userEnrolments.enrolments
      .filter { e ⇒ e.confidenceLevel >= minConfidence }
      .flatMap { e ⇒ e getIdentifier "UTR" }
      .map { utr ⇒ utr.value }
      .headOption
  }


}
