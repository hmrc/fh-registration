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

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.Json
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraDataRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

class SubmissionExtraDataController @Inject()(
  val submissionDataRepository: SubmissionExtraDataRepository)
  extends BaseController {


  def saveBusinessRegistrationDetails(userId: String, formTypeRef: String) = Action.async(parse.json[BusinessRegistrationDetails]) {
    request ⇒
      submissionDataRepository
        .saveBusinessRegistrationDetails(userId, formTypeRef, request.body)
        .map(_ ⇒ Ok)
        .recover(onRepositoryError)
  }

  def updateFormId(userId: String, formTypeRef: String, formId: String) = Action.async {
    submissionDataRepository
      .updateFormId(userId, formTypeRef, formId)
      .map(found ⇒
        if (found) Ok
        else NotFound)
      .recover(onRepositoryError)
  }

  val onRepositoryError: PartialFunction[Throwable, Result] = {
    case e: Throwable ⇒ BadGateway(Json.toJson(s"Mongo Db error ${e.getMessage}"))
  }

}
