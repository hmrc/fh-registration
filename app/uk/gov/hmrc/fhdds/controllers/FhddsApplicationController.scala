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

import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.fhdds.connectors.DfsStoreConnector
import uk.gov.hmrc.fhdds.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhdds.models.dfsStore.Submission
import uk.gov.hmrc.fhdds.repositories.{SubmissionExtraData, SubmissionExtraDataRepository}
import uk.gov.hmrc.fhdds.services.FhddsApplicationService
import uk.gov.hmrc.play.http.NotFoundException
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class FhddsApplicationController @Inject() (
  val dfsStoreConnector: DfsStoreConnector,
  val submissionDataRepository: SubmissionExtraDataRepository,
  val applicationService: FhddsApplicationService
)
  extends BaseController {

  def getApplication(submissionRef: String) = Action.async {
    for {
      submission ← dfsStoreConnector.getSubmission(submissionRef)
      extraData ← findSubmissionExtraData(submission)
      application = createDesSubmission(submission, extraData)
    } yield {
      Ok(Json.toJson(application))
    }
  }

  private def createDesSubmission(submission: Submission, extraData: SubmissionExtraData) = {
    val xml = scala.xml.XML.loadString(submission.formData)
    val data = scalaxb.fromXML[generated.Data](xml)
    applicationService.iformXmlToApplication(data, extraData.businessRegistrationDetails)
  }

  private def findSubmissionExtraData(submission: Submission) = {
    submissionDataRepository
      .findSubmissionExtraData(submission.formId)
      .map(_ getOrElse (throw new NotFoundException("extra data not found for formId")))
  }

}
