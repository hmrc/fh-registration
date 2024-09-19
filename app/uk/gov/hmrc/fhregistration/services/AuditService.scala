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

package uk.gov.hmrc.fhregistration.services

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import play.api.libs.json.JsValue
import play.api.{Configuration, Environment}
import uk.gov.hmrc.fhregistration.models.fhdds.{DeregistrationRequest, SubmissionRequest, WithdrawalRequest}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent

@ImplementedBy(classOf[DefaultAuditService])
trait AuditService {

  def buildSubmissionCreateAuditEvent(
    submissionRequest: SubmissionRequest,
    safeId: String,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent

  def buildSubmissionAmendAuditEvent(
    submissionRequest: SubmissionRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent

  def buildSubmissionWithdrawalAuditEvent(
    withdrawalRequest: WithdrawalRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent

  def buildSubmissionDeregisterAuditEvent(
    deregistrationRequest: DeregistrationRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent

}

@Singleton
class DefaultAuditService @Inject() (
  val http: HttpClientV2,
  val configuration: Configuration,
  environment: Environment
) extends AuditService {

  val auditSource = "fhdds"
  val auditEmailSource = "fhdds-send-email"
  val auditType = "fulfilmentHouseRegistrationSubmission"

  override def buildSubmissionCreateAuditEvent(
    submissionRequest: SubmissionRequest,
    safeId: String,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent =
    buildSubmissionAuditEvent(
      submissionRequest.submission,
      registrationNumber,
      s"/fulfilment-diligence/subscription/$safeId/id-type/safe"
    )

  def buildSubmissionAmendAuditEvent(
    submissionRequest: SubmissionRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent =
    buildSubmissionAuditEvent(
      submissionRequest.submission,
      registrationNumber,
      s"/fulfilment-diligence/subscription/$registrationNumber/id-type/fhdds"
    )

  def buildSubmissionWithdrawalAuditEvent(
    withdrawalRequest: WithdrawalRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent =
    buildSubmissionAuditEvent(
      withdrawalRequest.withdrawal,
      registrationNumber,
      s"/fulfilment-diligence/subscription/$registrationNumber/withdrawal"
    )

  def buildSubmissionDeregisterAuditEvent(
    deregistrationRequest: DeregistrationRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent =
    buildSubmissionAuditEvent(
      deregistrationRequest.deregistration,
      registrationNumber,
      s"/fulfilment-diligence/subscription/$registrationNumber/deregister"
    )

  private def buildSubmissionAuditEvent(
    data: JsValue,
    registrationNumber: String,
    path: String
  )(implicit hc: HeaderCarrier): DataEvent = {

    val additionalDetails: Seq[(String, String)] = Seq(
      "submissionRef"  -> registrationNumber,
      "submissionData" -> data.toString()
    )

    DataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(s"FHDDS - $registrationNumber", path),
      detail = hc.toAuditDetails(additionalDetails: _*)
    )

  }

}
