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

import java.text.SimpleDateFormat
import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.fhregistration.actions.Actions
import uk.gov.hmrc.fhregistration.connectors.{DesConnector, EmailConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.fhregistration.models.TaxEnrolmentsCallback
import uk.gov.hmrc.fhregistration.models.des.DesStatus
import uk.gov.hmrc.fhregistration.models.des.DesStatus.DesStatus
import uk.gov.hmrc.fhregistration.models.fhdds.FhddsStatus.FhddsStatus
import uk.gov.hmrc.fhregistration.models.fhdds._
import uk.gov.hmrc.fhregistration.repositories.{SubmissionTracking, SubmissionTrackingRepository}
import uk.gov.hmrc.fhregistration.services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


class FhddsApplicationController @Inject()(
  val desConnector: DesConnector,
  val taxEnrolmentConnector: TaxEnrolmentConnector,
  val emailConnector: EmailConnector,
  val submissionTrackingRepository: SubmissionTrackingRepository,
  val auditService: AuditService,
  val auditConnector: AuditConnector,
  val actions: Actions)
  extends BaseController {

  import actions._

  val SubmissionTrackingAgeThresholdMs = 60 * 60 * 1000L

  def subscribe(safeId: String, currentRegNumber: Option[String]) = userGroupAction.async(parse.json[SubmissionRequest]) { implicit r ⇒
    val request = r.body
    for {
      desResponse ← desConnector.sendSubmission(safeId, request.submission)(hc)
      response = SubmissionResponse(desResponse.registrationNumberFHDDS, desResponse.processingDate)
    } yield {
      Logger.info(s"Received registration number ${desResponse.registrationNumberFHDDS} for safeId $safeId")

      currentRegNumber foreach { regNumber ⇒
        taxEnrolmentConnector.deleteGroupEnrolment(r.groupId, regNumber)
      }

      val event: DataEvent = auditService.buildSubmissionCreateAuditEvent(request, safeId, response.registrationNumber)
      saveSubscriptionTracking(
        safeId,
        r.userId,
        desResponse.etmpFormBundleNumber,
        request.emailAddress
      ) andThen { case _ ⇒
        subscribeToTaxEnrolment(safeId, desResponse.etmpFormBundleNumber)
      }

      auditSubmission(response.registrationNumber, event)

      Ok(Json toJson response)
    }
  }

  def enrolmentProgress() = userAction.async { implicit request ⇒
    val now = System.currentTimeMillis
    submissionTrackingRepository
      .findSubmissionTrackingByUserId(request.userId)
      .map {
        case Some(tracking) if (now - tracking.submissionTime) > SubmissionTrackingAgeThresholdMs ⇒
          Logger.error(s"Submission tracking is too old for user ${request.userId}. Was made at ${tracking.submissionTime}")
          Ok(Json.toJson(EnrolmentProgress.Pending))
        case Some(_)                                                                              ⇒
          Ok(Json.toJson(EnrolmentProgress.Pending))
        case None                                                                                 ⇒
          Ok(Json.toJson(EnrolmentProgress.Unknown))
      }
  }

  def saveSubscriptionTracking(safeId: String, userId: String, etmpFormBundleNumber: String, emailAddress: String): Future[Any] = {
    val submissionTracking = SubmissionTracking(
      userId,
      etmpFormBundleNumber,
      emailAddress,
      System.currentTimeMillis()
    )

    val result = submissionTrackingRepository.insertSubmissionTracking(submissionTracking)
    result.onComplete({
      case Success(r) ⇒ Logger.info(s"Submission tracking record saved for $safeId and etmpFormBundleNumber $etmpFormBundleNumber")
      case Failure(e) ⇒ Logger.error(s"Submission tracking record FAILED for $safeId and etmpFormBundleNumber $etmpFormBundleNumber", e)
    })

    result

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
      sendEmail(request.emailAddress)

      Ok(Json toJson response)
    }
  }

  def withdrawal(fhddsRegistrationNumber: String) = userGroupAction.async(parse.json[WithdrawalRequest]) { implicit r ⇒
    val request = r.body
    for {
      desResponse ← desConnector.sendWithdrawal(fhddsRegistrationNumber, request.withdrawal)(hc)
      processingDate = desResponse.processingDate
    } yield {
      val event = auditService.buildSubmissionWithdrawalAuditEvent(
        request, fhddsRegistrationNumber)
      auditSubmission(fhddsRegistrationNumber, event)
      sendEmail(
        request.emailAddress,
        emailTemplateId = emailConnector.withdrawalEmailTemplateID,
        emailParameters = Map("withdrawalDate" → new SimpleDateFormat("dd MMMM yyyy").format(processingDate)))

      Ok(Json toJson processingDate)
    }
  }

  def sendEmail(email: String, emailTemplateId: String = emailConnector.defaultEmailTemplateID, emailParameters: Map[String, String] = Map.empty)(implicit hc: HeaderCarrier, request: Request[AnyRef]) = {
    import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
    emailConnector
      .sendEmail(
        emailTemplateId = emailTemplateId,
        userData = UserData(email),
        emailParameters)(hc, request, MdcLoggingExecutionContext.fromLoggingDetails)
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

  def subscriptionCallback(formBundleId: String) = Action.async(parse.json[TaxEnrolmentsCallback]) { implicit request ⇒
    val data = request.body
    Logger.info(s"Received subscription callback for formBundleId: $formBundleId with data: $data")
    if (data.succeeded) {

      submissionTrackingRepository
        .findSubmissionTrakingByFormBundleId(formBundleId)
        .map {
          case None ⇒ Logger.error(s"Could not find enrolment tracking data for bundleId $formBundleId")
          case Some(tracking) ⇒
            sendEmail(tracking.email)
            deleteSubmissionTrackingAfterCallback(formBundleId)
        }
        .map(_ ⇒ Ok(""))
        .recover { case _ ⇒ Ok("") }

    } else {
      Logger.error(s"Tax enrolment failed for $formBundleId: ${data.errorResponse} ($data)")
      Future successful Ok("")
    }
  }

  private def deleteSubmissionTrackingAfterCallback(formBundleId: String) = {
    submissionTrackingRepository
      .deleteSubmissionTackingByFormBundleId(formBundleId)
      .andThen {
        case Success(1) ⇒ Logger.info(s"Submission tracking deleted for $formBundleId")
        case Success(0) ⇒ Logger.warn(s"Submission tracking not found for $formBundleId")
        case Success(n) ⇒ Logger.error(s"Submission tracking delete for $formBundleId returned an unexpected number of docs: $n")
        case Failure(e) ⇒ Logger.error(s"Submission tracking delete failed for $formBundleId", e)
      }
  }

  private def sendConfirmationEmailAfterCallback(formBundleId: String)(implicit request: Request[AnyRef]) = {
    val result = submissionTrackingRepository
      .findSubmissionTrakingByFormBundleId(formBundleId)
      .map {
        case Some(tracking) ⇒ sendEmail(tracking.email)
        case None ⇒ Logger.error(s"Could not find enrolment tracking data for bundleId $formBundleId")
      }

    result.onFailure {
      case t ⇒ Logger.error(s"Error retrieving enrolment tracking data for bundeIld $formBundleId", t)
    }

    result

  }

  def checkStatus(fhddsRegistrationNumber: String) = Action.async { implicit request ⇒
    desConnector
      .getStatus(fhddsRegistrationNumber)(hc)
      .map {_.subscriptionStatus }
      .map { mdtpSubscriptionStatus}
      .map {status ⇒ Ok(Json toJson status)}
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

  def mdtpSubscriptionStatus(desStatus: DesStatus): FhddsStatus = {
    import DesStatus._
    desStatus match {

      case InProcessing
           | SentToDs
           | DsOutcomeInProgress
           | SentToRcm            ⇒ FhddsStatus.Processing

      case RegFormReceived        ⇒ FhddsStatus.Received
      case Successful             ⇒ FhddsStatus.Approved
      case ApprovedWithConditions ⇒ FhddsStatus.ApprovedWithConditions
      case Rejected               ⇒ FhddsStatus.Rejected
      case Revoked                ⇒ FhddsStatus.Revoked
      case Withdrawal             ⇒ FhddsStatus.Withdrawn
      case Deregistered           ⇒ FhddsStatus.Deregistered
      case _                      ⇒
        Logger.error(s"Unknown status received from des: $desStatus")
        throw new IllegalArgumentException(s"des status: $desStatus")
    }

  }

}
