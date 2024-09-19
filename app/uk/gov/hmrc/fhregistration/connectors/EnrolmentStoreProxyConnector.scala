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
import play.api.libs.json.{JsObject, Json}
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultEnrolmentStoreProxyConnector @Inject() (
  val http: HttpClientV2,
  val configuration: Configuration,
  environment: Environment
)(implicit ec: ExecutionContext)
    extends ServicesConfig(configuration) with EnrolmentStoreProxyConnector with Logging {

  val serviceBaseUrl = s"${baseUrl("enrolment-store-proxy")}/enrolment-store-proxy"
  lazy val serviceName = config("tax-enrolments").getOptional[String]("serviceName").getOrElse("HMRC-OBTDS-ORG")

  private def enrolmentKey(registrationNumber: String): String =
    s"$serviceName~ETMPREGISTRATIONNUMBER~$registrationNumber"

  private def es8Url(groupId: String, registrationNumber: String) =
    s"$serviceBaseUrl/enrolment-store/groups/$groupId/enrolments/${enrolmentKey(registrationNumber)}"
  private def es11Url(userId: String, registrationNumber: String) =
    s"$serviceBaseUrl/enrolment-store/users/$userId/enrolments/${enrolmentKey(registrationNumber)}"
  private def es12Url(userId: String, registrationNumber: String) =
    s"$serviceBaseUrl/enrolment-store/users/$userId/enrolments/${enrolmentKey(registrationNumber)}"

  private def es2Url(userId: String) = s"$serviceBaseUrl/enrolment-store/users/$userId/enrolments"
  private def es3Url(groupId: String) = s"$serviceBaseUrl/enrolment-store/groups/$groupId/enrolments"

  override def allocateEnrolmentToGroup(userId: String, groupId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    logger.info(s"Request to alocate enrolment to group id")
    val jsonRequest = Json.obj(
      "userId" -> userId,
      "type"   -> "principal",
      "action" -> "enrolAndActivate"
    )
    http
      .post(url"${es8Url(groupId, registrationNumber)}")
      .withBody[JsObject](jsonRequest)
      .execute[HttpResponse]
  }

  override def allocateEnrolmentToUser(userId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    http
      .post(url"${es11Url(userId, registrationNumber)}")
      .execute[HttpResponse]
  }

  override def deassignEnrolmentFromUser(userId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    http
      .delete(url"${es12Url(userId, registrationNumber)}")
      .execute[HttpResponse]
  }

  override def userEnrolments(userId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {
    http
      .get(url"${es2Url(userId)}")
      .execute[JsObject]
  }

  override def groupEnrolments(groupId: String)(implicit hc: HeaderCarrier): Future[JsObject] =
    http
      .get(url"${es3Url(groupId)}")
      .execute[JsObject]

}

@ImplementedBy(classOf[DefaultEnrolmentStoreProxyConnector])
trait EnrolmentStoreProxyConnector extends HttpErrorFunctions {

  // ES8
  def allocateEnrolmentToGroup(userId: String, groupId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse]

  // ES11
  def allocateEnrolmentToUser(userId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse]

  // ES12
  def deassignEnrolmentFromUser(userId: String, registrationNumber: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse]

  // Es2
  def userEnrolments(userId: String)(implicit hc: HeaderCarrier): Future[JsObject]

  // Es3
  def groupEnrolments(groupId: String)(implicit hc: HeaderCarrier): Future[JsObject]
}
