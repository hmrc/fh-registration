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

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import uk.gov.hmrc.fhdds.config.WSHttp
import uk.gov.hmrc.fhdds.models.dfsStore.Submission
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class DfsStoreConnectorImpl extends DfsStoreConnector with ServicesConfig {
  override val dfsStoreBaseUrl = baseUrl("dfs-store")
  val http = WSHttp
}

@ImplementedBy(classOf[DfsStoreConnectorImpl])
trait DfsStoreConnector {
  implicit val hc = new HeaderCarrier()

  val dfsStoreBaseUrl: String
  val http: WSHttp

  def getSubmission(submissionRef: String): Future[Submission] = {
    val url = s"$dfsStoreBaseUrl/dfs-store/submissions/$submissionRef"
    http.GET[Submission](url)
  }

}
