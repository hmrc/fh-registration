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

package uk.gov.hmrc.fhregistration.connectors

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import play.api.Mode.Mode
import play.api.libs.json.JsValue
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.fhregistration.models.des.{DesSubmissionResponse, DesWithdrawalResponse, StatusResponse}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@ImplementedBy(classOf[DefaultDesConnector])
trait DesConnector {
  def getStatus(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[StatusResponse]
  def sendSubmission(safeId: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse]
  def sendAmendment(fhddsRegistrationNumber: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse]
  def sendWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(hc: HeaderCarrier): Future[DesWithdrawalResponse]
  def display(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[HttpResponse]
}

@Singleton
class DefaultDesConnector @Inject() (
  val http: HttpClient,
  val runModeConfiguration: Configuration,
  environment: Environment
) extends DesConnector with ServicesConfig {

  def desServiceUri: String = config("des-service").getString("uri").getOrElse("")
  def desServiceBaseUri: String = config("des-service").getString("baseuri").getOrElse("")
  def desServiceStatusUri = s"${baseUrl("des-service")}$desServiceBaseUri"

  def desSubmissionUrl(safeId: String) =s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/id/$safeId/id-type/safe"
  def desAmendmentUrl(fhddsRegistrationNumber: String) =s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/id/$fhddsRegistrationNumber/id-type/fhdds"
  def desWithdrawalUrl(fhddsRegistrationNumber: String) =s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/$fhddsRegistrationNumber/withdrawal"

  val desToken: String = config("des-service").getString("authorization-token").getOrElse("")
  val environmentKey: String = config("des-service").getString("environment").getOrElse("")

  override protected def mode: Mode = environment.mode

  private def headerCarrierBuilder(hc: HeaderCarrier) = {
    hc.copy(authorization = Some(Authorization(s"Bearer $desToken"))).withExtraHeaders("Environment" -> environmentKey)
  }

  def getStatus(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[StatusResponse] = {
    implicit val desHeaders: HeaderCarrier = headerCarrierBuilder(hc)
    http.GET[StatusResponse](s"$desServiceStatusUri/fulfilment-diligence/subscription/$fhddsRegistrationNumber/status")
  }

  def sendSubmission(safeId: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse] = {
    Logger.info(s"Sending fhdds registration data to DES for safeId $safeId")
    implicit val desHeaders: HeaderCarrier = headerCarrierBuilder(hc)
    http.POST[JsValue, DesSubmissionResponse](desSubmissionUrl(safeId), submission)
  }

  def sendAmendment(fhddsRegistrationNumber: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse] = {
    Logger.info(s"Sending fhdds amendment data to DES for regNumber $fhddsRegistrationNumber")
    implicit val desHeaders: HeaderCarrier = headerCarrierBuilder(hc)
    http.POST[JsValue, DesSubmissionResponse](desAmendmentUrl(fhddsRegistrationNumber), submission)
  }

  def sendWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(hc: HeaderCarrier): Future[DesWithdrawalResponse] = {
    Logger.info(s"Sending fhdds withdrawal data to DES for regNumber $fhddsRegistrationNumber")
    implicit val desHeaders: HeaderCarrier = headerCarrierBuilder(hc)
    http.PUT[JsValue, DesWithdrawalResponse](desWithdrawalUrl(fhddsRegistrationNumber), submission)
  }

  def display(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val desHeaders: HeaderCarrier = headerCarrierBuilder(hc)
    http.GET(s"$desServiceStatusUri/fulfilment-diligence/subscription/$fhddsRegistrationNumber/get")
  }
}