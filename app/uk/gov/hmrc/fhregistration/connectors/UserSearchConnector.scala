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
import play.api.libs.json.JsObject
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultUserSearchConnector @Inject() (
  val http: HttpClientV2,
  val configuration: Configuration,
  environment: Environment
)(implicit ec: ExecutionContext)
    extends ServicesConfig(configuration) with UserSearchConnector with Logging {

  val serviceBaseUrl = s"${baseUrl("user-search")}/users-groups-search"

  private def userInfoUrl(userId: String) = s"$serviceBaseUrl/users/$userId"

  private def groupInfoUrl(groupId: String) = s"$serviceBaseUrl/groups/$groupId/users"

  override def retrieveUserInfo(userId: String)(implicit hc: HeaderCarrier): Future[JsObject] = {
    logger.info(s"Request to user groups search")
    http.get(url"${userInfoUrl(userId)}").execute[JsObject]
  }

  override def retrieveGroupInfo(groupId: String)(implicit hc: HeaderCarrier): Future[List[JsObject]] =
    http.get(url"${groupInfoUrl(groupId)}").execute[List[JsObject]]

}

@ImplementedBy(classOf[DefaultUserSearchConnector])
trait UserSearchConnector extends HttpErrorFunctions {

  def retrieveUserInfo(userId: String)(implicit hc: HeaderCarrier): Future[JsObject]

  def retrieveGroupInfo(groupId: String)(implicit hc: HeaderCarrier): Future[List[JsObject]]
}
