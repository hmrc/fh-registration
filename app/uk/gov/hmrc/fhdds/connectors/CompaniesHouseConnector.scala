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

import uk.gov.hmrc.fhdds.config.WSHttp
import uk.gov.hmrc.fhdds.models.companyHouse.CompanySearchResult.companySearchResultReader
import uk.gov.hmrc.fhdds.models.companyHouse.{Company, CompanySearchResult, Officer, OfficersSearchResult}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object CompaniesHouseConnector extends CompaniesHouseConnector {
  override val authHeader = CompaniesHouseConfig.authHeader
  override val url = CompaniesHouseConfig.url
  override val http = WSHttp
}

trait CompaniesHouseConnector {
  val http: WSHttp
  val url: String
  val authHeader: String

  def fuzzySearchCompany(title: String): Future[List[Company]] = {
    implicit val hc = HeaderCarrier(authorization = Some(Authorization(authHeader)))
    http.GET[CompanySearchResult](s"$url/search/companies", Seq("q" → title)).map(_.items)
  }

  def registeredOfficers(companyNumber: String): Future[List[Officer]] = {
    implicit val hc = HeaderCarrier(authorization = Some(Authorization(authHeader)))
    http.GET[OfficersSearchResult](s"$url/company/$companyNumber/officers").map(_.items)
  }


}
