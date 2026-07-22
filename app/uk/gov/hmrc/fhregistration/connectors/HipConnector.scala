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

import com.google.inject.{ImplementedBy, Inject}
import play.api.libs.json.{JsValue, Reads}
import play.api.libs.ws.writeableOf_JsValue
import play.api.{Configuration, Logging}
import sttp.model.HeaderNames
import uk.gov.hmrc.fhregistration.models.des.*
import uk.gov.hmrc.fhregistration.models.hip.*
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

final case class HipSubmissionException(statusCode: Int, code: String, reason: String) extends RuntimeException(reason)

@ImplementedBy(classOf[DefaultHipConnector])
trait HipConnector extends HttpErrorFunctions {

  def getStatus(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[SubscriptionStatusResponse]

  def subscriptionDisplay(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[HttpResponse]

  def subscriptionWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesWithdrawalResponse]

  def createOrUpdateFhdds(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesSubmissionResponse]

  def subscriptionDeregistration(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesDeregistrationResponse]

}

class DefaultHipConnector @Inject() (http: HttpClientV2, configuration: Configuration)(implicit
  val ec: ExecutionContext
) extends ServicesConfig(configuration) with HipConnector with Logging {

  private val hipServer = baseUrl("hip")
  private[connectors] val hipServiceBasePath = s"$hipServer/etmp/RESTAdapter"

  private def headerCarrierBuilder(hc: HeaderCarrier) = hc.copy(authorization = None)

  private lazy val clientId: String =
    getString("microservice.services.hip.clientId")

  private lazy val clientSecret: String =
    getString("microservice.services.hip.clientSecret")

  lazy val authSecret: String =
    Base64.getEncoder
      .encodeToString(
        s"$clientId:$clientSecret".getBytes(StandardCharsets.UTF_8)
      )

  private def hipHeaders: Seq[(String, String)] = Seq(
    HeaderNames.Authorization -> s"Basic $authSecret",
    "X-Originating-System"    -> "fh-registration",
    "X-Receipt-Date"          -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
    "X-Transmitting-System"   -> "HIP"
  )

  private[connectors] def correlationId(implicit hc: HeaderCarrier): String =
    randomUUID.toString

  def subscriptionDisplayUrl(fhddsRegistrationNumber: String) =
    s"$hipServiceBasePath/fulfilment-diligence/subscription/$fhddsRegistrationNumber"

  def subscriptionWithdrawalUrl(fhddsRegistrationNumber: String) =
    s"$hipServiceBasePath/fulfilment-diligence/subscription/withdrawal/$fhddsRegistrationNumber"

  def createOrUpdateFhddsUrl(id: String, idType: String) =
    s"$hipServiceBasePath/fulfilment-diligence/subscription/id/$id/id-type/$idType"

  def subscriptionDeregistrationUrl(fhddsRegistrationNumber: String) =
    s"$hipServiceBasePath/fulfilment-diligence/subscription/deregistration/$fhddsRegistrationNumber"

  def getStatusUrl(fhddsRegistrationNumber: String, idType: String, regime: String) =
    s"$hipServiceBasePath/subscription-status?idNumber=$fhddsRegistrationNumber&idType=$idType&regime=$regime"

  private[connectors] def logIfError(response: HttpResponse): HttpResponse =
    response.status match {
      case 200 | 201 =>
        response
      case _ =>
        logger.error(s"Received error ${response.status} from HIP with message - ${response.body}")
        response

    }

  private[connectors] def logAndThrowExceptionIfError(response: HttpResponse): HttpResponse =
    response.status match {
      case 200 | 201 =>
        response
      case _ =>
        logger.error(s"Received error ${response.status} from HIP with message - ${response.body}")
        throw UpstreamErrorResponse(s"${response.status} received from HIP", response.status)

    }

  private def hipErrorResponse(response: HttpResponse): HipErrorResponse =
    Try(response.json.as[HipErrorResponse]).getOrElse(HipErrorResponse("UNKNOWN_HIP_ERROR", response.body))

  private[connectors] def customHipSubmissionOrAmendmentRead[A: Reads](response: HttpResponse): A =
    response.status match {
      case status if is2xx(status) =>
        response.json.as[A]
      case status if is4xx(status) =>
        val error = hipErrorResponse(response)
        logger.warn(s"Received error ${response.status} from HIP with code ${error.code} and reason ${error.reason}")
        throw HipSubmissionException(status, error.code, error.reason)
      case status =>
        logger.error(s"Received error $status from HIP with message - ${response.body}")
        throw UpstreamErrorResponse(s"$status received from HIP", status, 502)
    }

  override def subscriptionDisplay(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[HttpResponse] = {
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    http
      .get(url"${subscriptionDisplayUrl(fhddsRegistrationNumber)}")
      .setHeader(("correlationid", correlationId) +: hipHeaders *)
      .execute[HttpResponse]
      .map(logIfError)
  }

  override def subscriptionWithdrawal(fhddsRegistrationNumber: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesWithdrawalResponse] = {
    logger.info(s"Sending fhdds withdrawal data to HIP for regNumber $fhddsRegistrationNumber")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    http
      .put(url"${subscriptionWithdrawalUrl(fhddsRegistrationNumber)}")
      .setHeader(("correlationid", correlationId) +: hipHeaders *)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(logAndThrowExceptionIfError)
      .map(_.json.as[HipWithdrawalResponse])
      .map(hr => DesWithdrawalResponse(hr.processingDate))
  }

  override def createOrUpdateFhdds(id: String, submission: JsValue)(
    hc: HeaderCarrier
  ): Future[DesSubmissionResponse] = {
    logger.info(s"Sending fhdds registration data to HIP for safeId $id")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    val idType = "fhdds"
    http
      .post(url"${createOrUpdateFhddsUrl(id, idType)}")
      .setHeader(("correlationid", correlationId) +: hipHeaders *)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(customHipSubmissionOrAmendmentRead[HipSubmissionResponse])
      .map(hr => DesSubmissionResponse(hr.processingDate, hr.etmpFormBundleNumber, hr.registrationNumberFHDDS))
  }

  override def subscriptionDeregistration(fhddsRegistrationNumber: String, submission: JsValue)(implicit
    hc: HeaderCarrier
  ): Future[DesDeregistrationResponse] = {
    logger.info(s"Sending fhdds deregistration data to HIP for regNumber $fhddsRegistrationNumber")
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    http
      .post(url"${subscriptionDeregistrationUrl(fhddsRegistrationNumber)}")
      .setHeader(("correlationid", correlationId) +: hipHeaders *)
      .withBody[JsValue](submission)
      .execute[HttpResponse]
      .map(logIfError)
      .map(_.json.as[HipDeregistrationResponse])
      .map(hr => DesDeregistrationResponse(hr.processingDate))
  }

  override def getStatus(fhddsRegistrationNumber: String)(hc: HeaderCarrier): Future[SubscriptionStatusResponse] = {
    implicit val headerCarrier: HeaderCarrier = headerCarrierBuilder(hc)
    val idType = "fhddsRegistrationNumber"
    val regime = "FHDDS"
    http
      .get(url"${getStatusUrl(fhddsRegistrationNumber, idType, regime)}")
      .setHeader(("correlationid", correlationId) +: hipHeaders *)
      .execute[HttpResponse]
      .map(logAndThrowExceptionIfError)
      .map(_.json.as[SubscriptionStatusResponse])
  }
}
