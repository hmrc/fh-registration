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

package uk.gov.hmrc.fhregistration.controllers

import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.fhregistration.config.MicroserviceAuditConnector
import uk.gov.hmrc.fhregistration.connectors.{DesConnector, EmailConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.fhregistration.models.fhdds.{SubmissionRequest, SubmissionResponse, UserData, WithdrawalRequest}
import uk.gov.hmrc.fhregistration.services.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class FhddsApplicationController @Inject()(
  val desConnector: DesConnector,
  val taxEnrolmentConnector: TaxEnrolmentConnector,
  val emailConnector: EmailConnector,
  val auditService: AuditService)
  extends BaseController {

  val auditConnector: AuditConnector = MicroserviceAuditConnector

  def subscribe(safeId: String) = Action.async(parse.json[SubmissionRequest]) { implicit r ⇒
    val request = r.body
    for {
      desResponse ← desConnector.sendSubmission(safeId, request.submission)(hc)
      response = SubmissionResponse(desResponse.registrationNumberFHDDS, desResponse.processingDate)
    } yield {
      Logger.info(s"Received registration number ${desResponse.registrationNumberFHDDS} for safeId $safeId")

      subscribeToTaxEnrolment(
        safeId,
        desResponse.etmpFormBundleNumber)

      val event = auditService.buildSubmissionCreateAuditEvent(
        request, safeId, response.registrationNumber)
      auditSubmission(response.registrationNumber, event)
      sendEmail(request.emailAddress, response.registrationNumber)

      Ok(Json toJson response)
    }
  }

  def amend(fhddsRegistrationNumber: String) = Action.async(parse.json[SubmissionRequest]) { implicit r ⇒
    val request = r.body
    for {
      desResponse ← desConnector.sendAmendment(fhddsRegistrationNumber, request.submission)(hc)
      response = SubmissionResponse(desResponse.registrationNumberFHDDS, desResponse.processingDate)
    } yield {
      val event = auditService.buildSubmissionAmendAuditEvent(
        request, response.registrationNumber)
      auditSubmission(response.registrationNumber, event)
      sendEmail(request.emailAddress, response.registrationNumber)

      Ok(Json toJson response)
    }
  }

  def withdrawal(fhddsRegistrationNumber: String) = Action.async(parse.json[WithdrawalRequest]) { implicit r ⇒
    val request = r.body
    for {
      desResponse ← desConnector.sendWithdrawal(fhddsRegistrationNumber, request.withdrawal)(hc)
      processingDate = desResponse.processingDate
    } yield {
      val event = auditService.buildSubmissionWithdrawalAuditEvent(
        request, fhddsRegistrationNumber)
      auditSubmission(fhddsRegistrationNumber, event)
      sendEmail(request.emailAddress, fhddsRegistrationNumber)

      Ok(Json toJson processingDate)
    }
  }

  def sendEmail(email: String, submissionRef: String)(implicit hc: HeaderCarrier, request: Request[AnyRef]) = {
    val emailTemplateId = emailConnector.defaultEmailTemplateID
    import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
    emailConnector
      .sendEmail(
        emailTemplateId = emailTemplateId,
        userData = UserData(email = email, submissionReference = submissionRef))(hc, request, MdcLoggingExecutionContext.fromLoggingDetails)

  }

  private def auditSubmission(registrationNumber: String, event: DataEvent)(implicit hc: HeaderCarrier) = {

    Logger.info(s"Sending audit event for registrationNumber $registrationNumber")

    import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
    auditConnector
      .sendEvent(event)(hc, MdcLoggingExecutionContext.fromLoggingDetails)
      .map(auditResult ⇒ Logger.info(s"Received audit result $auditResult for registrationNumber $registrationNumber"))
      .recover {
        case t: Throwable ⇒ Logger.error(s"Audit failed for registrationNumber $registrationNumber", t)
      }

  }

  private def subscribeToTaxEnrolment(safeId: String, etmpFormBundleNumber: String)(implicit hc: HeaderCarrier) = {
    Logger.info(s"Sending subscription for safeId = $safeId for etmpFormBundelNumber = $etmpFormBundleNumber to tax enrolments")
    taxEnrolmentConnector
     .subscribe(safeId, etmpFormBundleNumber)(hc)
     .onComplete({
       case Success(r) ⇒ Logger.info(s"Tax enrolments for subscription $safeId and etmpFormBundleNumber $etmpFormBundleNumber returned $r")
       case Failure(e) ⇒ Logger.error(s"Tax enrolments for subscription $safeId and etmpFormBundleNumber $etmpFormBundleNumber failed", e)
     })
  }

  def subscriptionCallback = Action { request ⇒
    val subscriptionId = request.getQueryString("subscriptionId").getOrElse("N/A")
    Logger.info(s"Received subscription callback for subscriptionId: $subscriptionId with response")
    Ok("")
  }

  def checkStatus(fhddsRegistrationNumber: String) = Action.async { implicit request ⇒
    desConnector.getStatus(fhddsRegistrationNumber)(hc) map { resp ⇒
      val dfsResponseStatus = resp.status
      Logger.info(s"Statue for $fhddsRegistrationNumber is $dfsResponseStatus")
      dfsResponseStatus match {
        case 200 ⇒ mdtpSubscriptionStatus(resp)
        case 400 ⇒ BadRequest("Submission has not passed validation. Invalid parameter FHDDS Registration Number.")
        case 404 ⇒ NotFound("No SAP Number found for the provided FHDDS Registration Number.")
        case 403 ⇒ Forbidden("Unexpected business error received.")
        case _   ⇒ InternalServerError("DES is currently experiencing problems that require live service intervention.")
      }
    }
  }

  def get(fhddsRegistrationNumber: String) = Action.async { implicit request ⇒
    desConnector.display(fhddsRegistrationNumber)(hc) map { resp ⇒
      val dfsResponseStatus = resp.status
      Logger.info(s"Got back subscription data for $fhddsRegistrationNumber with status $dfsResponseStatus")
      dfsResponseStatus match {
        case 200 ⇒ Ok(resp.json)
        case 400 ⇒ BadRequest("Submission has not passed validation. Invalid parameter FHDDS Registration Number.")
        case 404 ⇒ NotFound("No SAP Number found for the provided FHDDS Registration Number.")
        case 403 ⇒ Forbidden("Unexpected business error received.")
        case _   ⇒ BadGateway("DES is currently experiencing problems that require live service intervention.")
      }
    }
  }

  def mdtpSubscriptionStatus(r: HttpResponse) = {
    val responseInJs = r.json
    (responseInJs \ "subscriptionStatus").as[String] match {
      case ("Reg Form Received")                                                             => Ok("Received")
      case ("Sent To DS") | ("DS Outcome In Progress") | ("In processing") | ("Sent to RCM") => Ok("Processing")
      case ("Successful")                                                                    => Ok("Successful")
      case ("Rejected")                                                                      => Ok("Rejected")
      case _                                                                                 => BadGateway("Unexpected business error received.")
    }
  }

}
