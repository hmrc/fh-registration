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
import uk.gov.hmrc.fhdds.connectors.{DesConnector, DfsStoreConnector}
import uk.gov.hmrc.fhdds.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhdds.models.fhdds.{SubmissionRequest, SubmissionResponse}
import uk.gov.hmrc.fhdds.repositories.{SubmissionExtraData, SubmissionExtraDataRepository}
import uk.gov.hmrc.fhdds.services.FhddsApplicationService
import uk.gov.hmrc.play.http.NotFoundException
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class FhddsApplicationController @Inject()(
  val dfsStoreConnector       : DfsStoreConnector,
  val desConnector            : DesConnector,
  val submissionDataRepository: SubmissionExtraDataRepository,
  val applicationService      : FhddsApplicationService
)
  extends BaseController {

  def getApplication(submissionRef: String) = Action.async {
    for {
      submission ← dfsStoreConnector.getSubmission(submissionRef)
      extraData ← findSubmissionExtraData(submission.formId)
      application = createDesSubmission(submission.formData, extraData)
    } yield {
      Ok(Json.toJson(application))
    }
  }

  def submit() = Action.async(parse.json[SubmissionRequest]) {implicit r ⇒
    val request = r.body
    for {
      extraData ← findSubmissionExtraData(request.formId)
      application = createDesSubmission(request.formData, extraData)
      safeId = extraData.businessRegistrationDetails.safeId
      desResponse ← desConnector.sendSubmission(safeId, application)(hc)
      response = SubmissionResponse(desResponse.registrationNumberFHDDS)
    } yield {
      Ok(Json toJson response)
    }

  }

  private def createDesSubmission(formData: String, extraData: SubmissionExtraData) = {
    val xml = scala.xml.XML.loadString(formData)
    val data = scalaxb.fromXML[generated.Data](xml)
    applicationService.iformXmlToApplication(data, extraData.businessRegistrationDetails)
  }

  def getSafeId(submissionRef: String) = Action.async {
    for {
      submission ← dfsStoreConnector.getSubmission(submissionRef)
      extraData ← findSubmissionExtraData(submission.formId)
      brd = extraData.businessRegistrationDetails
    } yield {
      Ok(Json toJson brd.safeId)
    }
  }

  private def findSubmissionExtraData(formId: String) = {
    submissionDataRepository
      .findSubmissionExtraData(formId)
      .map(_ getOrElse (throw new NotFoundException("extra data not found for formId")))
  }

}
