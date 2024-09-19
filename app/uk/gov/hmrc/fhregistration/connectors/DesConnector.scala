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

package uk.gov.hmrc.fhregistration.connectors

import com.google.inject.ImplementedBy
import play.api.libs.json.JsValue
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.fhregistration.models.des.{DesDeregistrationResponse, DesSubmissionResponse, DesWithdrawalResponse, StatusResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultDesConnector])
trait DesConnector extends HttpErrorFunctions {
  def getStatus(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[StatusResponse]
  def sendSubmission(safeId: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse]
  def sendAmendment(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesSubmissionResponse]
  def sendWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesWithdrawalResponse]
  def sendDeregistration(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesDeregistrationResponse]
  def display(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[HttpResponse]
}

@Singleton
class DefaultDesConnector @Inject() (
  val http: HttpClientV2,
  val configuration: Configuration,
  environment: Environment,
  servicesConfig: ServicesConfig
)(implicit val ec: ExecutionContext)
    extends ServicesConfig(configuration) with DesConnector with Logging {

  def desServiceUri: String = config("des-service").getOptional[String]("uri").getOrElse("")
  def desServiceBaseUri: String = config("des-service").getOptional[String]("baseuri").getOrElse("")
  def desServiceStatusUri = s"${baseUrl("des-service")}$desServiceBaseUri"

  def desSubmissionUrl(safeId: String) =
    s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/id/$safeId/id-type/safe"
  def desAmendmentUrl(fhddsRegistrationNumber: String) =
    s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/id/$fhddsRegistrationNumber/id-type/fhdds"
  def desWithdrawalUrl(fhddsRegistrationNumber: String) =
    s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/$fhddsRegistrationNumber/withdrawal"
  def desDeregisterUrl(fhddsRegistrationNumber: String) =
    s"${baseUrl("des-service")}$desServiceBaseUri$desServiceUri/$fhddsRegistrationNumber/deregistration"

  val desToken: String = config("des-service").getOptional[String]("authorization-token").getOrElse("")
  val environmentKey: String = config("des-service").getOptional[String]("environment").getOrElse("")

  private def headerCarrierBuilder(hc: HeaderCarrier) = hc.copy(authorization = None)

  private val desAuthorizationHeader = "Authorization" -> s"Bearer $desToken"
  private val desEnvironmentHeader = "Environment" -> environmentKey

  private[connectors] def customDESRead(response: HttpResponse): HttpResponse =
    response.status match {
      case 403 =>
        logger.error(s"Received error 403 from DES with message - ${response.json}")
        response
      case 429 =>
        logger.error("[RATE LIMITED] Received 429 from DES - converting to 503")
        throw UpstreamErrorResponse("429 received from DES - converted to 503", 429, 503)
      case _ =>
        response
    }

  def getStatus(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[StatusResponse] = {
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    val url = s"$desServiceStatusUri/fulfilment-diligence/subscription/$fhddsRegistrationNumber/status"
    http
      .get(url"$url")
      .execute[HttpResponse]
      .map(customDESRead)
      .map(_.json.as[StatusResponse])
  }

  def sendSubmission(safeId: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse] = {
    logger.info(s"Sending fhdds registration data to DES for safeId $safeId")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)

    http
      .post(url"${desSubmissionUrl(safeId)}")
      .setHeader(desAuthorizationHeader)
      .setHeader(desEnvironmentHeader)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(customDESRead)
      .map(_.json.as[DesSubmissionResponse])
  }

  def sendAmendment(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesSubmissionResponse] = {
    logger.info(s"Sending fhdds amendment data to DES for regNumber $fhddsRegistrationNumber")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)

    http
      .post(url"${desAmendmentUrl(fhddsRegistrationNumber)}")
      .setHeader(desAuthorizationHeader)
      .setHeader(desEnvironmentHeader)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(customDESRead)
      .map(_.json.as[DesSubmissionResponse])
  }

  def sendWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesWithdrawalResponse] = {
    logger.info(s"Sending fhdds withdrawal data to DES for regNumber $fhddsRegistrationNumber")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)

    http
      .put(url"${desWithdrawalUrl(fhddsRegistrationNumber)}")
      .setHeader(desAuthorizationHeader)
      .setHeader(desEnvironmentHeader)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(customDESRead)
      .map(_.json.as[DesWithdrawalResponse])
  }

  def sendDeregistration(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesDeregistrationResponse] = {
    logger.info(s"Sending fhdds deregistration data to DES for regNumber $fhddsRegistrationNumber")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)

    http
      .post(url"${desDeregisterUrl(fhddsRegistrationNumber)}")
      .setHeader(desAuthorizationHeader)
      .setHeader(desEnvironmentHeader)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(customDESRead)
      .map(_.json.as[DesDeregistrationResponse])
  }

  def display(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    val url = s"$desServiceStatusUri/fulfilment-diligence/subscription/$fhddsRegistrationNumber/get"
    http
      .get(url"$url")
      .setHeader(desAuthorizationHeader)
      .setHeader(desEnvironmentHeader)
      .execute[HttpResponse]
      .map(customDESRead)
  }
}
