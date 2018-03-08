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

package uk.gov.hmrc.fhregistration.services

import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhregistration.models.des.{DesSubmissionResponse, SubScriptionCreate}
import uk.gov.hmrc.fhregistration.models.fhdds.{SubmissionRequest, UserData}
import uk.gov.hmrc.fhregistration.repositories.SubmissionExtraData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import javax.inject.Singleton
import com.google.inject.ImplementedBy

import scala.concurrent.ExecutionContext

@Singleton
class AuditServiceImpl extends AuditService {

}

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {

  val auditSource = "fhdds"
  val auditEmailSource = "fhdds-send-email"
  val auditType = "fulfilmentHouseRegistrationSubmission"

  val failed = "fulfilmentHouseEmailSentFail"
  val successful = "fulfilmentHouseEmailSentSuccess"

  def buildSubmissionAuditEvent(
    submissionRequest : SubmissionRequest,
    desResponse       : DesSubmissionResponse,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val details = JsObject(Seq(
      "authorization" → JsString(hc.authorization map (_.value) getOrElse ""),
      "submissionRef" → JsString(registrationNumber),
      "submissionData" → submissionRequest.submission
    ))

    val customTags = Map(
      "path" → Some(s"/fulfilment-diligence/subscription/${submissionRequest.safeId}"),
      "clientIP" -> hc.trueClientIp,
      "clientPort" -> hc.trueClientPort,
      "X-Request-Chain" → Some(hc.requestChain.value),
      "X-Session-ID" → hc.sessionId.map(_.value),
      "X-Request-ID" → hc.requestId.map(_.value),
      "deviceID" → hc.deviceID,
      "transactionName" -> Some(s"FHDDS - $registrationNumber")
    ) collect {
      case (key, Some(value)) ⇒ key -> value
    }

    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.headers.toMap ++ customTags,
      detail = details
    )

  }

  def sendEmailSuccessEvent(userData: UserData)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    sendEvent(successful, Map("user-data" -> userData.toString(), "service-action" -> "send-email"))

  def sendEmailFailureEvent(userData: UserData, error: Throwable)(implicit hc: HeaderCarrier, ec: ExecutionContext): Unit =
    sendEvent(failed, Map("user-data" -> userData.toString(), "error" -> error.toString(), "service-action" -> "send-email"))

  private def sendEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    eventFor(auditType, detail)

  private def eventFor(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier) =
    DataEvent(
      auditSource = auditEmailSource,
      auditType = auditType,
      tags = hc.headers.toMap,
      detail = detail)

}