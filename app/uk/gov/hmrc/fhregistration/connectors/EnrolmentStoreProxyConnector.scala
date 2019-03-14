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

import com.google.inject.ImplementedBy
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.libs.json.{JsObject, Json}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultEnrolmentStoreProxyConnector @Inject()(
                                            val http: HttpClient,
                                            val runModeConfiguration: Configuration,
                                            environment: Environment
                                          ) extends EnrolmentStoreProxyConnector with ServicesConfig {

  override protected def mode: Mode = environment.mode

  val serviceBaseUrl = s"${baseUrl("enrolment-store-proxy")}/enrolment-store-proxy"
  lazy val serviceName = config("tax-enrolments").getString("serviceName").getOrElse("HMRC-OBTDS-ORG")

  private def enrolmentKey(registrationNumber: String): String = s"$serviceName~ETMPREGISTRATIONNUMBER~$registrationNumber"

  private def es8Url(groupId: String, registrationNumber: String) = s"$serviceBaseUrl/enrolment-store/groups/$groupId/enrolments/${enrolmentKey(registrationNumber)}"
  private def es11Url(userId: String, registrationNumber: String) = s"$serviceBaseUrl/enrolment-store/users/$userId/enrolments/${enrolmentKey(registrationNumber)}"
  private def es12Url(userId: String, registrationNumber: String) = s"$serviceBaseUrl/enrolment-store/users/$userId/enrolments/${enrolmentKey(registrationNumber)}"

  override def allocateEnrolmentToGroup(userId: String, groupId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    Logger.info(s"Request to alocate enrolment to group id")
    val jsonRequest = Json.obj(
      "userId" -> userId,
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    )
    http.POST[JsObject, HttpResponse](es8Url(groupId, registrationNumber), jsonRequest)
  }

  override def allocateEnrolmentToUser(userId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POSTEmpty(es11Url(userId, registrationNumber))
  }

  override def deassignEnrolmentFromUser(userId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE(es12Url(userId, registrationNumber))
  }

}

@ImplementedBy(classOf[DefaultEnrolmentStoreProxyConnector])
trait EnrolmentStoreProxyConnector extends HttpErrorFunctions {

  //ES8
  def allocateEnrolmentToGroup(userId: String, groupId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  //ES11
  def allocateEnrolmentToUser(userId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse]

  //ES12
  def deassignEnrolmentFromUser(userId: String, registrationNumber: String)(implicit hc: HeaderCarrier): Future[HttpResponse]
}

