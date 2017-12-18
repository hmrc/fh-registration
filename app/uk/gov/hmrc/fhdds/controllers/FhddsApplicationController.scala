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

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.fhdds.config.MicroserviceAuditConnector
import uk.gov.hmrc.fhdds.connectors.{DesConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.fhdds.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhdds.models.des.{DesSubmissionResponse, SubScriptionCreate}
import uk.gov.hmrc.fhdds.models.fhdds.{SubmissionRequest, SubmissionResponse}
import uk.gov.hmrc.fhdds.repositories.{SubmissionExtraData, SubmissionExtraDataRepository}
import uk.gov.hmrc.fhdds.services.{AuditService, ControllerServices, FhddsApplicationService}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class FhddsApplicationController @Inject()(
  val desConnector            : DesConnector,
  val taxEnrolmentConnector   : TaxEnrolmentConnector,
  val submissionDataRepository: SubmissionExtraDataRepository,
  val applicationService      : FhddsApplicationService,
  val auditService            : AuditService
)
  extends BaseController {

  val auditConnector: AuditConnector = MicroserviceAuditConnector

  def submit() = Action.async(parse.json[SubmissionRequest]) {implicit r ⇒
    val request = r.body
    for {
      extraData ← findSubmissionExtraData(request.formId)
      application = createDesSubmission(request.formData, extraData)
      safeId = extraData.businessRegistrationDetails.safeId
      desResponse ← desConnector.sendSubmission(safeId, application)(hc)
      response = SubmissionResponse(ControllerServices.createSubmissionRef())
    } yield {
      Logger.info(s"Received subscription id ${desResponse.registrationNumberFHDDS} for safeId $safeId")
      storeRegistrationNumber(
        request.formId,
        response.registrationNumber,
        desResponse.registrationNumberFHDDS
      )
      subscribeToTaxEnrolment(
        desResponse.registrationNumberFHDDS,
        extraData.businessRegistrationDetails.safeId,
        extraData.authorization)
      auditSubmission(request, application, extraData, desResponse, response.registrationNumber)
      Ok(Json toJson response)
    }
  }

  private def auditSubmission(
    submissionRequest: SubmissionRequest,
    application: SubScriptionCreate,
    extraData: SubmissionExtraData,
    desResponse: DesSubmissionResponse,
    submissionRef: String
  ) (implicit hc: HeaderCarrier)= {

    Logger.info(s"Sending audit event for submissionRef $submissionRef")
    val event = auditService.buildSubmissionAuditEvent(
      submissionRequest, application, extraData, desResponse, submissionRef)
    import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
    auditConnector
      .sendExtendedEvent(event)(hc, MdcLoggingExecutionContext.fromLoggingDetails)
      .map(auditResult ⇒ Logger.info(s"Received audit result $auditResult for submissionRef $submissionRef"))
      .recover{
        case t: Throwable ⇒ Logger.error(s"Audit failed for submissionRef $submissionRef $submissionRef", t)
      }

  }
  private def storeRegistrationNumber(formId: String, submissionRef: String, registrationNumberFHDDS: String) = {
    submissionDataRepository
      .updateRegistrationNumber(formId, submissionRef, registrationNumberFHDDS)
      .map(v ⇒ Logger.info(s"Saving registration number yield $v"))
      .recover {
        case t: Throwable ⇒ Logger.error("Saving registration number failed", t)
      }
  }

  private def subscribeToTaxEnrolment(subscriptionId: String, safeId: String, authorization: Option[String])(implicit hc: HeaderCarrier) = {
    Logger.info(s"Sending subscription $subscriptionId for $safeId to tax enrolments")
    taxEnrolmentConnector
     .subscribe(subscriptionId, safeId, authorization)(hc)
     .onComplete({
       case Success(r) ⇒ Logger.info(s"Tax enrolments for subscription $subscriptionId and safeId $safeId returned $r")
       case Failure(e) ⇒ Logger.error(s"Tax enrolments for subscription $subscriptionId and safeId $safeId failed", e)
     })
  }

  def subscriptionCallback = Action { request ⇒
    val subscriptionId = request.getQueryString("subscriptionId").getOrElse("N/A")
    Logger.info(s"Received subscription callback for subscriptionId: $subscriptionId with response" )
    Ok("")
  }

  private def createDesSubmission(formData: String, extraData: SubmissionExtraData) = {
    val xml = scala.xml.XML.loadString(formData)
    val data = scalaxb.fromXML[generated.Data](xml)
    applicationService.iformXmlToApplication(data, extraData.businessRegistrationDetails)
  }

  private def findSubmissionExtraData(formId: String) = {
    submissionDataRepository
      .findSubmissionExtraDataByFormId(formId)
      .map(_ getOrElse (throw new NotFoundException("extra data not found for formId")))
  }

}
