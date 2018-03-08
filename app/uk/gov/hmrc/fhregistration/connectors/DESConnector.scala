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

import com.google.inject.ImplementedBy
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue}
import uk.gov.hmrc.fhregistration.config.WSHttp
import uk.gov.hmrc.fhregistration.models.des.DesSubmissionResponse
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class DesConnectorImpl extends DesConnector with ServicesConfig {

  def desServiceUri = config("des-service").getString("uri").getOrElse("")
  def desServiceBaseUri() = config("des-service").getString("baseuri").getOrElse("")
  def desServiceStatusUri = s"${baseUrl("des-service")}${desServiceBaseUri()}"
  def desSubmissionUrl(safeId: String) =s"${baseUrl("des-service")}$desServiceUri/$safeId"

  lazy val http: WSHttp = WSHttp

  override val desToken = config("des-service").getString("authorization-token").getOrElse("")
  override val environment = config("des-service").getString("environment").getOrElse("")
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {

  val http: WSHttp
  val environment: String
  val desToken: String
  def desServiceUri: String
  def desServiceStatusUri: String
  def desSubmissionUrl(safeId: String): String

  def getStatus(fhddsRegistrationNumber: String)(headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val desHeaders = headerCarrier.copy(authorization = Some(Authorization(s"Bearer $desToken"))).withExtraHeaders("Environment" -> environment)
    http.GET(s"$desServiceStatusUri/fulfilment-diligence/subscription/$fhddsRegistrationNumber/status")
  }


  def sendSubmission(safeId: String, submission: JsValue)(hc: HeaderCarrier): Future[DesSubmissionResponse] = {
    Logger.info(s"Sending fhdds registration data to DES for safeId $safeId")

    implicit val desHeaders = hc.copy(authorization = Some(Authorization(s"Bearer $desToken"))).withExtraHeaders("Environment" -> environment)
    http.POST[JsValue, DesSubmissionResponse](desSubmissionUrl(safeId), submission)
  }

  def display(fhddsRegistrationNumber: String)(headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val desHeaders = headerCarrier.copy(authorization = Some(Authorization(s"Bearer $desToken"))).withExtraHeaders("Environment" -> environment)
    http.GET(s"$desServiceStatusUri/fulfilment-diligence/subscription/$fhddsRegistrationNumber/get")
  }
}