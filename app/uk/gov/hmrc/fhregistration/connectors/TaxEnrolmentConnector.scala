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
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhregistration.config.WSHttp
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig

import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class TaxEnrolmentConnectorImpl extends TaxEnrolmentConnector with ServicesConfig {
  lazy val http: WSHttp = WSHttp

  val callback = config("tax-enrolments").getString("callback").getOrElse("http://")
  val serviceName = config("tax-enrolments").getString("serviceName").getOrElse("HMRC-OBTDS-ORG")
  override def subscriberUrl(subscriptionId: String) =
    s"${baseUrl("tax-enrolments")}/tax-enrolments/subscriptions/$subscriptionId/subscriber"
}

@ImplementedBy(classOf[TaxEnrolmentConnectorImpl])
trait TaxEnrolmentConnector {
  val http: WSHttp
  val callback: String
  val serviceName: String

  def subscriberUrl(subscriptionId: String): String


  def subscribe(subscriptionId: String, safeId: String, authorization: Option[String])(hc: HeaderCarrier): Future[Option[JsObject]] = {
    val extraHeaders = authorization map ("Authorization" -> _)
    implicit val hcWithAuthorization = hc.withExtraHeaders(extraHeaders.toSeq: _*)
    http.PUT[JsObject, Option[JsObject]](
      subscriberUrl(subscriptionId),
      requestBody(safeId)
    )
  }

  def requestBody(safeId: String): JsObject = {
    Json.obj(
      "serviceName" → JsString(serviceName),
      "callback" → JsString(callback),
      "etmpId" → JsString(safeId)
    )
  }
}