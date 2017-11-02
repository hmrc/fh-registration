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

import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.fhdds.config.WSHttp
import uk.gov.hmrc.fhdds.connectors.CompaniesHouseConfig.config
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DESConnector extends DESConnect {
  val DEServiceUrl: String = config("des-service").getString("url").getOrElse("")
  val orgLookupURI: String = "registration/organisation"
  val urlHeaderEnvironment: String = config("des-service").getString("environment").getOrElse("")
  val urlHeaderAuthorization: String = s"Bearer ${config("des-service").getString("authorization-token").getOrElse("")}"
  val http = WSHttp
}

trait DESConnect extends ServicesConfig {

  val DEServiceUrl: String
  val orgLookupURI: String
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String
  val http: WSHttp

  val lookupData: JsObject = Json.obj(
    "regime" -> "ITSA",
    "requiresNameMatch" -> false,
    "isAnAgent" -> false
  )

  def lookup(utr: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    http.POST[JsValue, HttpResponse](s"$DEServiceUrl/$orgLookupURI/utr/$utr", Json.toJson(lookupData)).map { response =>
      if (response.status != 200) {
        Logger.warn(s"[DESConnect][lookup] - status: ${response.status}")
      }
      response
    }
  }

  def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))
}