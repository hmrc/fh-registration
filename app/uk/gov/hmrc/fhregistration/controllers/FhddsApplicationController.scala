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

import generated.fhdds.{LimitedDataFormat, PartnershipDataFormat, SoleDataFormat}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Request}
import uk.gov.hmrc.fhregistration.config.MicroserviceAuditConnector
import uk.gov.hmrc.fhregistration.connectors.{DesConnector, EmailConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhregistration.models.des.DesSubmissionResponse
import uk.gov.hmrc.fhregistration.models.fhdds.{SubmissionRequest, SubmissionResponse, UserData}
import uk.gov.hmrc.fhregistration.repositories.{SubmissionExtraData, SubmissionExtraDataRepository}
import uk.gov.hmrc.fhregistration.services.{AuditService, ControllerServices, FhddsApplicationService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class FhddsApplicationController @Inject()(
  val desConnector: DesConnector,
  val taxEnrolmentConnector: TaxEnrolmentConnector,
  val emailConnector: EmailConnector,
  val submissionDataRepository: SubmissionExtraDataRepository,
  val applicationService: FhddsApplicationService,
  val auditService: AuditService)
  extends BaseController {

  val auditConnector: AuditConnector = MicroserviceAuditConnector

  def submit() = Action.async(parse.json[SubmissionRequest]) { implicit r ⇒
    val request = r.body
    for {
      desResponse ← desConnector.sendSubmission(request.safeId, request.submission)(hc)
      response = SubmissionResponse(ControllerServices.createSubmissionRef())
    } yield {
      Logger.info(s"Received subscription id ${desResponse.registrationNumberFHDDS} for safeId ${request.safeId}")
      subscribeToTaxEnrolment(
        request.safeId,
        desResponse.etmpFormBundleNumber)

      auditSubmission(request, desResponse, response.registrationNumber)
      Ok(Json toJson response)
    }
  }

  def sendEmail(email: Option[String], submissionRef: String)(implicit hc: HeaderCarrier, request: Request[AnyRef]) = {
    email match {
      case Some(mail) => {
        val emailTemplateId = emailConnector.defaultEmailTemplateID
        import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
        emailConnector
          .sendEmail(
            emailTemplateId = emailTemplateId,
            userData = UserData(email = mail, submissionReference = submissionRef))(hc, request, MdcLoggingExecutionContext.fromLoggingDetails)
      }
      case None       =>
        Logger.warn(s"Unable to retrieve email address for $submissionRef")
    }
  }

  private def auditSubmission(
    submissionRequest: SubmissionRequest,
    desResponse: DesSubmissionResponse,
    submissionRef: String
  )(implicit hc: HeaderCarrier) = {

    Logger.info(s"Sending audit event for submissionRef $submissionRef")
    val event = auditService.buildSubmissionAuditEvent(
      submissionRequest, desResponse, submissionRef)
    import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
    auditConnector
      .sendExtendedEvent(event)(hc, MdcLoggingExecutionContext.fromLoggingDetails)
      .map(auditResult ⇒ Logger.info(s"Received audit result $auditResult for submissionRef $submissionRef"))
      .recover {
        case t: Throwable ⇒ Logger.error(s"Audit failed for submissionRef $submissionRef $submissionRef", t)
      }

  }

  private def storeRegistrationNumberAndBundleNumber(formId: String, submissionRef: String,
                                                     etmpFormBundleNumber: String,
                                                     registrationNumberFHDDS: String) = {
    submissionDataRepository
      .updateRegistrationNumberWithETMPFormBundleNumber(formId,
        submissionRef, etmpFormBundleNumber, registrationNumberFHDDS)
      .map(v ⇒ Logger.info(s"Saving registration number yield $v"))
      .recover {
        case t: Throwable ⇒ Logger.error("Saving registration number failed", t)
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

  private def createDesSubmission(formData: String, extraData: SubmissionExtraData) = {
    val xml = scala.xml.XML.loadString(formData)
    extraData.businessRegistrationDetails.businessType.map(_.toLowerCase) match {
      case Some("sole trader")    ⇒
        val data = scalaxb.fromXML[generated.sole.Data](xml)
        applicationService.soleTraderSubmission(data, extraData.businessRegistrationDetails)
      case Some("corporate body") ⇒
        val data = scalaxb.fromXML[generated.limited.Data](xml)
        applicationService.limitedCompanySubmission(data, extraData.businessRegistrationDetails)
      case Some("partnership")    ⇒
        val data = scalaxb.fromXML[generated.partnership.Data](xml)
        applicationService.partnershipSubmission(data, extraData.businessRegistrationDetails)
    }
  }

  private def findSubmissionExtraData(formId: String) = {
    submissionDataRepository
      .findSubmissionExtraDataByFormId(formId)
      .map(_ getOrElse (throw new NotFoundException("extra data not found for formId")))
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

  def mdtpSubscriptionStatus(r: HttpResponse) = {
    val responseInJs = r.json
    (responseInJs \ "subscriptionStatus").as[String] match {
      case ("Reg Form Received")                                                             => Ok("Received")
      case ("Sent To DS") | ("DS Outcome In Progress") | ("In processing") | ("Sent to RCM") => Ok("Processing")
      case ("Successful")                                                                    => Ok("Successful")
      case ("Rejected")                                                                      => Ok("Rejected")
      case _                                                                                 => Unauthorized("Unexpected business error received.")
    }
  }

}
