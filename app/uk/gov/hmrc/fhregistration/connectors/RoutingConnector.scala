/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.connectors

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue
import uk.gov.hmrc.fhregistration.models.des.{DesDeregistrationResponse, DesSubmissionResponse, DesWithdrawalResponse}
import uk.gov.hmrc.fhregistration.models.des.StatusResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

@Singleton
class RoutingConnector @Inject() (
  configuration: Configuration,
  desConnector: DesConnector,
  hipConnector: HipConnector
) extends ServicesConfig(configuration) {

  val useHip: Boolean = getBoolean("feature.hip")

  def display(fhddsRegistrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    if (useHip)
      hipConnector.subscriptionDisplay(fhddsRegistrationNumber)(hc)
    else
      desConnector.display(fhddsRegistrationNumber)(hc)

  def sendDeregistration(fhddsRegistrationNumber: String, submission: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[DesDeregistrationResponse] =
    if (useHip)
      hipConnector.subscriptionDeregistration(fhddsRegistrationNumber, submission)(hc)
    else
      desConnector.sendDeregistration(fhddsRegistrationNumber, submission)(hc)

  def sendWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[DesWithdrawalResponse] =
    if (useHip)
      hipConnector.subscriptionWithdrawal(fhddsRegistrationNumber, submission)(hc)
    else
      desConnector.sendWithdrawal(fhddsRegistrationNumber, submission)(hc)

  def sendSubmission(safeId: String, submission: JsValue)(implicit hc: HeaderCarrier): Future[DesSubmissionResponse] =
    if (useHip)
      hipConnector.createOrUpdateFhdds(safeId, submission)(hc)
    else
      desConnector.sendSubmission(safeId, submission)(hc)

  def sendAmendment(fhddsRegistrationNumber: String, submission: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[DesSubmissionResponse] =
    if (useHip)
      hipConnector.createOrUpdateFhdds(fhddsRegistrationNumber, submission)(hc)
    else
      desConnector.sendAmendment(fhddsRegistrationNumber, submission)(hc)

  def getStatus(fhddsRegistrationNumber: String)(implicit hc: HeaderCarrier): Future[StatusResponse] =
    desConnector.getStatus(fhddsRegistrationNumber)(hc)
}
