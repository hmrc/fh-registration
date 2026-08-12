/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.implicits.*
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Request, Result}
import uk.gov.hmrc.fhregistration.actions.Actions
import uk.gov.hmrc.fhregistration.connectors.*
import uk.gov.hmrc.fhregistration.models.{IdType, TaxEnrolmentsCallback}
import uk.gov.hmrc.fhregistration.models.des.{DesDeregistrationResponse, DesStatus, DesSubmissionResponse, DesWithdrawalResponse, StatusResponse}
import uk.gov.hmrc.fhregistration.models.des.DesStatus.DesStatus
import uk.gov.hmrc.fhregistration.models.fhdds.FhddsStatus.FhddsStatus
import uk.gov.hmrc.fhregistration.models.fhdds.*
import uk.gov.hmrc.fhregistration.models.hip.HipStatus
import uk.gov.hmrc.fhregistration.repositories.DefaultSubmissionTrackingRepository
import uk.gov.hmrc.fhregistration.services.{AuditService, SubmissionTrackingService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.text.SimpleDateFormat
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class FhddsApplicationController @Inject() (
  val desConnector: DesConnector,
  val hipConnector: HipConnector,
  val configuration: ServicesConfig,
  val taxEnrolmentConnector: TaxEnrolmentConnector,
  val emailConnector: EmailConnector,
  val submissionTrackingService: SubmissionTrackingService,
  val auditService: AuditService,
  val auditConnector: AuditConnector,
  val cc: ControllerComponents,
  val actions: Actions,
  val repo: DefaultSubmissionTrackingRepository
)(implicit val ec: ExecutionContext)
    extends BackendController(cc) with Logging {

  import actions._

  def useHip: Boolean = configuration.getBoolean("features.hip")

  def withDownstream[A](hip: => Future[A], des: => Future[A]): Future[A] = if (useHip) hip else des

  def findAllSubmissions = Action.async { _ =>
    repo.findAll().map(submissionTrackingList => Ok(Json.toJson(submissionTrackingList)))
  }

  def getSubmission(formBundleId: String) = Action.async { _ =>
    repo.findSubmissionTrackingByFormBundleId(formBundleId).map(x => Ok(Json.toJson(x)))

  }

  def subscribe(safeId: String, currentRegNumber: Option[String]) =
    userGroupAction.async(parse.json[SubmissionRequest]) { implicit r =>
      val request = r.body
      (for {
        etmpResponse <- withDownstream(
                          hipConnector
                            .createOrUpdateFhdds(safeId, IdType.SAFE, request.submission)(hc)
                            .map(_.toDesSubmissionResponse),
                          desConnector.sendSubmission(safeId, request.submission)(hc)
                        )
        response = SubmissionResponse(etmpResponse.registrationNumberFHDDS, etmpResponse.processingDate)
      } yield {
        logger.info(s"Received registration number ${etmpResponse.registrationNumberFHDDS} for safeId $safeId")

        currentRegNumber foreach { regNumber =>
          taxEnrolmentConnector.deleteGroupEnrolment(r.groupId, regNumber)
        }

        val event: DataEvent =
          auditService.buildSubmissionCreateAuditEvent(request, safeId, response.registrationNumber)
        submissionTrackingService.saveSubscriptionTracking(
          safeId,
          r.userId,
          etmpResponse.etmpFormBundleNumber,
          request.emailAddress,
          response.registrationNumber
        ) andThen { case _ =>
          subscribeToTaxEnrolment(safeId, etmpResponse.etmpFormBundleNumber).failed.foreach { e =>
            submissionTrackingService
              .updateSubscriptionTracking(etmpResponse.etmpFormBundleNumber, EnrolmentProgress.Error)
          }
        }

        auditSubmission(response.registrationNumber, event)

        Ok(Json toJson response)
      }).recover(handleSubmissionError)
    }

  def enrolmentProgress = userAction.async { implicit request =>
    submissionTrackingService.enrolmentProgress(request.userId, request.registrationNumber) map { progress =>
      Ok(Json toJson progress)
    }
  }

  def amend(fhddsRegistrationNumber: String) = Action.async(parse.json[SubmissionRequest]) { implicit r =>
    val request = r.body
    (for {
      etmpResponse <- withDownstream(
                        hipConnector
                          .createOrUpdateFhdds(fhddsRegistrationNumber, IdType.FHDDS, request.submission)(hc)
                          .map(_.toDesSubmissionResponse),
                        desConnector.sendAmendment(fhddsRegistrationNumber, request.submission)(hc)
                      )
      response = SubmissionResponse(etmpResponse.registrationNumberFHDDS, etmpResponse.processingDate)
    } yield {
      val event = auditService.buildSubmissionAmendAuditEvent(request, response.registrationNumber)
      auditSubmission(response.registrationNumber, event)
      sendEmail(request.emailAddress)

      Ok(Json toJson response)
    }).recover(handleSubmissionError)
  }

  def withdrawal(fhddsRegistrationNumber: String) = userGroupAction.async(parse.json[WithdrawalRequest]) { implicit r =>
    val request = r.body
    for {
      etmpResponse <- withDownstream(
                        hipConnector
                          .subscriptionWithdrawal(fhddsRegistrationNumber, request.withdrawal)(hc)
                          .map(_.toDesWithdrawalResponse),
                        desConnector.sendWithdrawal(fhddsRegistrationNumber, request.withdrawal)(hc)
                      )
      processingDate = etmpResponse.processingDate
    } yield {
      val event = auditService.buildSubmissionWithdrawalAuditEvent(request, fhddsRegistrationNumber)
      auditSubmission(fhddsRegistrationNumber, event)
      sendEmail(
        request.emailAddress,
        emailTemplateId = emailConnector.withdrawalEmailTemplateID,
        emailParameters = Map("withdrawalDate" -> new SimpleDateFormat("dd MMMM yyyy").format(processingDate))
      )

      Ok(Json toJson processingDate)
    }
  }

  def deregister(fhddsRegistrationNumber: String) = userGroupAction.async(parse.json[DeregistrationRequest]) {
    implicit r =>
      val request = r.body
      for {
        etmpResponse <- withDownstream(
                          hipConnector
                            .subscriptionDeregistration(fhddsRegistrationNumber, request.deregistration)(hc)
                            .map(_.toDesDeregistrationResponse),
                          desConnector.sendDeregistration(fhddsRegistrationNumber, request.deregistration)(hc)
                        )
        processingDate = etmpResponse.processingDate
      } yield {
        val event = auditService.buildSubmissionDeregisterAuditEvent(request, fhddsRegistrationNumber)
        auditSubmission(fhddsRegistrationNumber, event)
        sendEmail(
          request.emailAddress,
          emailTemplateId = emailConnector.deregisterEmailTemplateID,
          emailParameters = Map("deregisterDate" -> new SimpleDateFormat("dd MMMM yyyy").format(processingDate))
        )

        Ok(Json toJson processingDate)
      }
  }

  def sendEmail(
    email: String,
    emailTemplateId: String = emailConnector.defaultEmailTemplateID,
    emailParameters: Map[String, String] = Map.empty
  )(implicit hc: HeaderCarrier, request: Request[AnyRef]) =
    emailConnector
      .sendEmail(emailTemplateId = emailTemplateId, userData = UserData(email), emailParameters)(using hc, request, ec)
      .failed
      .foreach { case t =>
        logger.error(s"Failed sending email $emailTemplateId", t)
      }

  private def auditSubmission(registrationNumber: String, event: DataEvent)(implicit hc: HeaderCarrier) = {

    logger.info(s"Sending audit event for registrationNumber $registrationNumber")

    auditConnector
      .sendEvent(event)(using hc, ec)
      .map(auditResult => logger.info(s"Received audit result $auditResult for registrationNumber $registrationNumber"))
      .recover { case t: Throwable =>
        logger.error(s"Audit failed for registrationNumber $registrationNumber", t)
      }

  }

  private def subscribeToTaxEnrolment(safeId: String, etmpFormBundleNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[?] = {
    logger.info(
      s"Sending subscription for safeId = $safeId for etmpFormBundelNumber = $etmpFormBundleNumber to tax enrolments"
    )
    taxEnrolmentConnector
      .subscribe(safeId, etmpFormBundleNumber)(using hc)
      .andThen {
        case Success(r) =>
          logger.info(
            s"Tax enrolments for subscription $safeId and etmpFormBundleNumber $etmpFormBundleNumber returned $r"
          )
        case Failure(e) =>
          logger
            .error(s"Tax enrolments for subscription $safeId and etmpFormBundleNumber $etmpFormBundleNumber failed", e)
      }
  }

  private def handleSubmissionError: PartialFunction[Throwable, Result] = {
    case DesSubmissionException(statusCode, code, reason) =>
      logger.warn(s"DES submission failed with status $statusCode and code $code: $reason")
      Status(statusCode)(Json.obj("code" -> code, "reason" -> reason))

    case HipSubmissionException(statusCode, code, reason) =>
      logger.warn(s"HIP submission failed with status $statusCode and code $code: $reason")
      Status(statusCode)(Json.obj("code" -> code, "reason" -> reason))
  }

  def subscriptionCallback(formBundleId: String) = Action.async(parse.json[TaxEnrolmentsCallback]) { implicit request =>
    val data = request.body
    logger.info(s"Received subscription callback for formBundleId: $formBundleId with data: $data")
    if (data.succeeded) {

      submissionTrackingService
        .getSubmissionTrackingEmail(formBundleId)
        .fold(logger.error(s"Could not find enrolment tracking data for bundleId $formBundleId"))(email =>
          sendEmail(email)
        )
        .andThen { case _ => submissionTrackingService.deleteSubmissionTracking(formBundleId) }
        .map(_ => Ok(""))
        .recover { case _ => Ok("") }

    } else {
      logger.error(s"Tax enrolment failed for $formBundleId: ${data.errorResponse} ($data)")
      Future successful Ok("")
    }
  }

  def deleteSubmission(formBundleId: String) = Action.async { _ =>
    submissionTrackingService
      .deleteSubmissionTracking(formBundleId)
      .map(_ => Ok(""))
      .recover { case _ => Ok(s"Submission with $formBundleId not found") }
  }

  def checkStatus(fhddsRegistrationNumber: String) = Action.async { implicit request =>
    withDownstream(
      hipConnector
        .getStatus(fhddsRegistrationNumber)(hc)
        .map(_.toDesStatusResponse),
      desConnector.getStatus(fhddsRegistrationNumber)(hc)
    )
      .map(_.subscriptionStatus)
      .map(mdtpSubscriptionStatus)
      .map { status =>
        Ok(Json toJson status)
      }
  }

  def get(fhddsRegistrationNumber: String) = Action.async { implicit request =>
    withDownstream(
      hipConnector.subscriptionDisplay(fhddsRegistrationNumber)(hc),
      desConnector.display(fhddsRegistrationNumber)(hc)
    ) map { resp =>
      val dfsResponseStatus = resp.status
      logger.info(s"Got back subscription data for $fhddsRegistrationNumber with status $dfsResponseStatus")
      dfsResponseStatus match {
        case 200 => Ok(resp.json)
        case 400 => BadRequest("Submission has not passed validation. Invalid parameter FHDDS Registration Number.")
        case 404 => NotFound("No SAP Number found for the provided FHDDS Registration Number.")
        case 422 => NotFound(s"Validation errors. ${resp.body}")
        case 403 => Forbidden("Unexpected business error received.")
        case _ =>
          logger.error(
            s"FhddsApplicationController.get - Unexpected error from ETMP API connector with status: ${resp.status} and body: ${resp.body}"
          )
          BadGateway("ETMP API is currently experiencing problems that require live service intervention.")
      }
    }
  }

  def mdtpSubscriptionStatus(desStatus: DesStatus): FhddsStatus = {
    import DesStatus._
    desStatus match {

      case InProcessing | SentToDs | DsOutcomeInProgress | SentToRcm => FhddsStatus.Processing

      case RegFormReceived        => FhddsStatus.Received
      case Successful             => FhddsStatus.Approved
      case ApprovedWithConditions => FhddsStatus.ApprovedWithConditions
      case Rejected               => FhddsStatus.Rejected
      case Revoked                => FhddsStatus.Revoked
      case Withdrawal             => FhddsStatus.Withdrawn
      case Deregistered           => FhddsStatus.Deregistered
      case _ =>
        logger.error(s"Unknown status received from des: $desStatus")
        throw new IllegalArgumentException(s"des status: $desStatus")
    }

  }

}
