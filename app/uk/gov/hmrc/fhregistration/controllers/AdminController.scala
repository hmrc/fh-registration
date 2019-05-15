/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.controllers

import com.google.inject.Inject
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.fhregistration.actions.Actions
import uk.gov.hmrc.fhregistration.connectors.{EnrolmentStoreProxyConnector, UserSearchConnector}
import uk.gov.hmrc.fhregistration.repositories.DefaultSubmissionTrackingRepository
import uk.gov.hmrc.fhregistration.services.{AuditService, SubmissionTrackingService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

class AdminController @Inject()(val submissionTrackingService: SubmissionTrackingService,
                                val auditService: AuditService,
                                val auditConnector: AuditConnector,
                                val actions: Actions,
                                val repo: DefaultSubmissionTrackingRepository,
                                val userSearchConnector: UserSearchConnector,
                                val cc: ControllerComponents,
                                val enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit val ec: ExecutionContext) extends BackendController(cc) {

  def findUserDetails(userId: String) = Action.async { implicit request =>

    for {
      response: JsObject <- userSearchConnector.retrieveUserInfo(userId)
    } yield {
      Ok(response)
    }
  }

  def findGroupDetails(groupId: String) = Action.async { implicit request =>
    for {
      response <- userSearchConnector.retrieveGroupInfo(groupId)
    } yield {
      Ok(Json.toJson(response))
    }
  }

  //ES8
  def allocateEnrolmentToGroup(userId: String, groupId: String, registrationNumber: String) = Action.async { implicit request =>
    for {
      response <- enrolmentStoreProxyConnector.allocateEnrolmentToGroup(userId, groupId, registrationNumber)
    } yield {
      Ok(response.body)
    }
  }

  //ES11
  def allocateEnrolmentToUser(userId: String, registrationNumber: String) = Action.async { implicit request =>
    for {
      response <- enrolmentStoreProxyConnector.allocateEnrolmentToUser(userId, registrationNumber)
    } yield {
      Ok(response.body)
    }
  }

  //ES12
  def deAssignEnrolment(userId: String, registrationNumber: String) = Action.async { implicit request =>
    for {
      response <- enrolmentStoreProxyConnector.deassignEnrolmentFromUser(userId, registrationNumber)
    } yield {
      Ok(response.body)
    }
  }

  def userEnrolments(userId: String) = Action.async {implicit request =>
    for{
      response <- enrolmentStoreProxyConnector.userEnrolments(userId)
    } yield {
      Ok(response)
    }
  }

  def groupEnrolments(groupId: String) = Action.async {implicit request =>
    for{
      response <- enrolmentStoreProxyConnector.groupEnrolments(groupId)
    } yield {
      Ok(response)
    }
  }


}
