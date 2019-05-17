/*
 * Copyright 2019 HM Revenue & Customs
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
import javax.inject.Inject
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class DefaultTaxEnrolmentConnector @Inject() (
  val http: HttpClient,
  val runModeConfiguration: Configuration,
  val runMode: RunMode,
  environment: Environment)(implicit val ec: ExecutionContext) extends ServicesConfig(runModeConfiguration, runMode) with TaxEnrolmentConnector {

  val callbackBase = config("tax-enrolments").getOptional[String]("callback").getOrElse("http://fh-registration.protected.mdtp:80/fhdds/tax-enrolment/callback/subscriptions")
  def callback(formBundleId: String) = s"$callbackBase/$formBundleId"

  val serviceBaseUrl =  s"${baseUrl("tax-enrolments")}/tax-enrolments"
  val serviceName = config("tax-enrolments").getOptional[String]("serviceName").getOrElse("HMRC-OBTDS-ORG")

  private def subscriberUrl(etmpFormBundleId: String) =
   s"$serviceBaseUrl/subscriptions/$etmpFormBundleId/subscriber"

  private def groupEnrolmentUrl(groupId: String, registrationNumber: String) = {
    val enrolmentKey = s"$serviceName~ETMPREGISTRATIONNUMBER~$registrationNumber"
    s"$serviceBaseUrl/groups/$groupId/enrolments/$enrolmentKey"
  }

  /**
    * Subscribe to tax enrolments
    * @param safeId - the id of the entity in ETMP
    * @param etmpFormBundleNumber - use this as the subscription id as requested by ETMP
    */
  override def subscribe(safeId: String, etmpFormBundleNumber: String)(implicit hc: HeaderCarrier): Future[Option[JsObject]] = {
    Logger.info(s"Request to tax enrolments authorisation header is present: ${hc.authorization.isDefined}")
    http.PUT[JsObject, Option[JsObject]](
      subscriberUrl(etmpFormBundleNumber),
      requestBody(safeId, etmpFormBundleNumber)
    )
  }

  override def deleteGroupEnrolment(groupId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[_] = {
    http.DELETE[HttpResponse](groupEnrolmentUrl(groupId, registrationNumber)).map { response ⇒
      if (is2xx(response.status)) response.body
      else throw new RuntimeException(s"Unexpected response code '${response.status}'")

    }
  }

  private def requestBody(etmpId: String, etmpFormBundleNumber: String): JsObject = {
    Json.obj(
      "serviceName" → JsString(serviceName),
      "callback" → JsString(callback(etmpFormBundleNumber)),
      "etmpId" → JsString(etmpId)
    )
  }
}

@ImplementedBy(classOf[DefaultTaxEnrolmentConnector])
trait TaxEnrolmentConnector extends HttpErrorFunctions {

  def subscribe(safeId: String, etmpFormBundleNumber: String)(implicit hc: HeaderCarrier): Future[Option[JsObject]]
  def deleteGroupEnrolment(groupId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[_]
}
