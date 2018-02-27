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

  val failed = "fhdds-send-email-failed"
  val successful = "fhdds-send-email-successful"

  def buildSubmissionAuditEvent(
    submissionRequest: SubmissionRequest,
    application: SubScriptionCreate,
    extraData: SubmissionExtraData,
    desResponse: DesSubmissionResponse,
    submissionRef: String
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {

    val details = JsObject(Seq(
      "authorization" → Json.toJson(extraData.authorization),
      "submissionRef" → JsString(submissionRef),
      "submissionData" → Json.toJson(application),
      "businessPartnerRecord" → Json.toJson(extraData.businessRegistrationDetails)
    ))

    val customTags = Map(
      "clientIP" -> hc.trueClientIp,
      "clientPort" -> hc.trueClientPort,
      "transactionName" -> Some(s"FHDDS - $submissionRef")
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