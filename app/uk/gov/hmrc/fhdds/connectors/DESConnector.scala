/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.fhdds.connectors

import com.google.inject.ImplementedBy
import play.api.Logger
import uk.gov.hmrc.fhdds.config.WSHttp
import uk.gov.hmrc.fhdds.models.des.{SubScriptionCreate, DesSubmissionResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpReads, HttpResponse}

import scala.concurrent.Future

class DesConnectorImpl extends DesConnector with ServicesConfig {

  def desServiceUri() = config("des-service").getString("uri").getOrElse("")
  def desSubmissionUrl(safeId: String) =s"${baseUrl("des-service")}${desServiceUri()}/$safeId"

  lazy val http: WSHttp = WSHttp

  override val desToken = config("des-service").getString("authorization-token").getOrElse("")
  override val environment = config("des-service").getString("environment").getOrElse("")
}

@ImplementedBy(classOf[DesConnectorImpl])
trait DesConnector {

  val http: WSHttp
  val environment: String
  val desToken: String

  def desSubmissionUrl(safeId: String): String
  def sendSubmission(safeId: String, application: SubScriptionCreate)(hc: HeaderCarrier): Future[DesSubmissionResponse] = {
    Logger.debug(s"Sending fhdds registration data to DES with payload $application")

    implicit val desHeaders = hc.copy(authorization = Some(Authorization(s"Bearer $desToken"))).withExtraHeaders("Environment" -> environment)
    http.POST[SubScriptionCreate, DesSubmissionResponse](desSubmissionUrl(safeId), application)
  }
}