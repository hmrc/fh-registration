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
import play.api.Mode.Mode
import play.api.libs.json.JsObject
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultUserSearchConnector @Inject()(
                                            val http: HttpClient,
                                            val runModeConfiguration: Configuration,
                                            environment: Environment
                                          ) extends UserSearchConnector with ServicesConfig {

  override protected def mode: Mode = environment.mode

  val serviceBaseUrl = s"${baseUrl("user-search")}/users-groups-search"

  private def userInfoUrl(userId: String) = s"$serviceBaseUrl/users/$userId"

  private def groupInfoUrl(groupId: String) = s"$serviceBaseUrl/groups/$groupId/users"

  override def retrieveUserInfo(userId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {
    Logger.info(s"Request to user groups search")
    http.GET[JsObject](userInfoUrl(userId))
  }

  override def retrieveGroupInfo(groupId: String)(implicit hc: HeaderCarrier): Future[List[JsObject]] = {
    http.GET[List[JsObject]](groupInfoUrl(groupId))
  }

}

@ImplementedBy(classOf[DefaultUserSearchConnector])
trait UserSearchConnector extends HttpErrorFunctions {

  def retrieveUserInfo(userId: String)(implicit hc: HeaderCarrier): Future[JsObject]

  def retrieveGroupInfo(groupId: String)(implicit hc: HeaderCarrier): Future[List[JsObject]]
}

