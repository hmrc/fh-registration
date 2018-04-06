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

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import uk.gov.hmrc.fhregistration.models.fhdds.{SubmissionRequest, WithdrawalRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.config.{AuditingConfig, BaseUri, Consumer}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject() (
  val http: HttpClient,
  val runModeConfiguration: Configuration,
  environment: Environment
) extends AuditConnector with ServicesConfig {

  override protected def mode: Mode = environment.mode

  val auditSource = "fhdds"
  val auditEmailSource = "fhdds-send-email"
  val auditType = "fulfilmentHouseRegistrationSubmission"

  val enableAuditing: Boolean = getBoolean(s"$env.auditing.enabled")

  val dataStreamHost: String = getString(s"$env.auditing.consumer.baseUri.host")
  val dataStreamPort: Int = getInt(s"$env.auditing.consumer.baseUri.port")
  val dataStreamProtocol: String = "http"

  val dataStreamBaseUri = BaseUri(dataStreamHost, dataStreamPort, dataStreamProtocol)

  override def auditingConfig: AuditingConfig = AuditingConfig(Some(Consumer(dataStreamBaseUri)), enableAuditing)

  def buildSubmissionCreateAuditEvent(
    submissionRequest : SubmissionRequest,
    safeId            : String,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent = {

    buildSubmissionAuditEvent(
      Left(submissionRequest),
      registrationNumber,
      s"/fulfilment-diligence/subscription/$safeId/id-type/safe")
  }

  def buildSubmissionAmendAuditEvent(
    submissionRequest : SubmissionRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent = {

    buildSubmissionAuditEvent(
      Left(submissionRequest),
      registrationNumber,
      s"/fulfilment-diligence/subscription/$registrationNumber/id-type/fhdds")
  }

  def buildSubmissionWithdrawalAuditEvent(
    withdrawalRequest : WithdrawalRequest,
    registrationNumber: String
  )(implicit hc: HeaderCarrier): DataEvent = {

    buildSubmissionAuditEvent(
      Right(withdrawalRequest),
      registrationNumber,
      s"/fulfilment-diligence/subscription/$registrationNumber/withdrawal")
  }

  private def buildSubmissionAuditEvent(
    submissionRequest : Either[SubmissionRequest, WithdrawalRequest],
    registrationNumber: String,
    path              : String

  )(implicit hc: HeaderCarrier): DataEvent = {

    val additionalDetails: Seq[(String, String)] = Seq(
      "submissionRef" → registrationNumber,
      "submissionData" → {
        submissionRequest.fold(
          submissionRequest ⇒ submissionRequest.submission.toString(),
          withdrawalRequest ⇒ withdrawalRequest.withdrawal.toString()
        )
      }
    )

    DataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = hc.toAuditTags(s"FHDDS - $registrationNumber", path),
      detail = hc.toAuditDetails(additionalDetails:_*)
    )

  }

}