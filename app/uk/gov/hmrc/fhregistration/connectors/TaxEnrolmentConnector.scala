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
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultTaxEnrolmentConnector @Inject() (
  val http: HttpClientV2,
  val configuration: Configuration,
  environment: Environment
)(implicit val ec: ExecutionContext)
    extends ServicesConfig(configuration) with TaxEnrolmentConnector with Logging {

  val callbackBase = config("tax-enrolments")
    .getOptional[String]("callback")
    .getOrElse("http://fh-registration.protected.mdtp:80/fhdds/tax-enrolment/callback/subscriptions")
  def callback(formBundleId: String) = s"$callbackBase/$formBundleId"

  val serviceBaseUrl = s"${baseUrl("tax-enrolments")}/tax-enrolments"
  val serviceName = config("tax-enrolments").getOptional[String]("serviceName").getOrElse("HMRC-OBTDS-ORG")

  private def subscriberUrl(etmpFormBundleId: String) =
    s"$serviceBaseUrl/subscriptions/$etmpFormBundleId/subscriber"

  private def groupEnrolmentUrl(groupId: String, registrationNumber: String) = {
    val enrolmentKey = s"$serviceName~ETMPREGISTRATIONNUMBER~$registrationNumber"
    s"$serviceBaseUrl/groups/$groupId/enrolments/$enrolmentKey"
  }

  /** Subscribe to tax enrolments
    * @param safeId
    *   \- the id of the entity in ETMP
    * @param etmpFormBundleNumber
    *   \- use this as the subscription id as requested by ETMP
    */
  override def subscribe(safeId: String, etmpFormBundleNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    logger.info(s"Request to tax enrolments authorisation header is present: ${hc.authorization.isDefined}")
    http
      .put(url"${subscriberUrl(etmpFormBundleNumber)}")
      .withBody[JsObject](requestBody(safeId, etmpFormBundleNumber))
      .execute[HttpResponse]
      .map { response =>
//        TODO: TIDY UP THIS if/else/throw
        if (is2xx(response.status)) {
          logger.info(s"Request to tax enrolments authorisation response: ${response.body}")
          response
        } else
          logger.warn(
            s"in tax enrolment subscribe, Unexpected response code '${response.status} with response body ${response.body}'"
          )
        throw new RuntimeException(
          s"in tax enrolment, Unexpected response code '${response.status} with response body ${response.body}'"
        )
      }
  }

  override def deleteGroupEnrolment(groupId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[_] =
    http
      .delete(url"${groupEnrolmentUrl(groupId, registrationNumber)}")
      .execute[HttpResponse]
      .map { response =>
        if (is2xx(response.status)) response.body
        else throw new RuntimeException(s"Unexpected response code '${response.status}'")
      }

  private def requestBody(etmpId: String, etmpFormBundleNumber: String): JsObject =
    Json.obj(
      "serviceName" -> JsString(serviceName),
      "callback"    -> JsString(callback(etmpFormBundleNumber)),
      "etmpId"      -> JsString(etmpId)
    )
}

@ImplementedBy(classOf[DefaultTaxEnrolmentConnector])
trait TaxEnrolmentConnector extends HttpErrorFunctions {

  def subscribe(safeId: String, etmpFormBundleNumber: String)(implicit hc: HeaderCarrier): Future[_]
  def deleteGroupEnrolment(groupId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[_]
}
