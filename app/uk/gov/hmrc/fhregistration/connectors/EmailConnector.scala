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
import javax.inject.Inject
import play.api.Mode.Mode
import play.api.mvc.Request
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.fhregistration.models.fhdds.{SendEmailRequest, UserData}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EmailConnectorImpl @Inject() (
  val http: HttpClient,
  val runModeConfiguration: Configuration,
  environment: Environment
) extends EmailConnector with ServicesConfig {

  override val emailUrl: String = baseUrl("email") + "/hmrc/email"
  override val defaultEmailTemplateID: String =  getConfString(s"email.defaultTemplateId", "fhdds_submission_confirmation")
  override val withdrawalEmailTemplateID: String =  getConfString(s"email.withdrawalEmailTemplateID", "fhdds_submission_withdrawal")
  override protected def mode: Mode = environment.mode
}

@ImplementedBy(classOf[EmailConnectorImpl])
trait EmailConnector {
  val http: HttpClient
  val emailUrl: String
  val defaultEmailTemplateID: String
  val withdrawalEmailTemplateID: String

  def sendEmail(emailTemplateId:String, userData: UserData, emailParameters: Map[String, String] = Map.empty)(implicit hc: HeaderCarrier, request: Request[AnyRef], ec: ExecutionContext): Future[Any] = {
    val toList: List[String] = List(userData.email)

    val email: SendEmailRequest = SendEmailRequest(templateId = emailTemplateId, to = toList, parameters = emailParameters, force = true)

    Logger.debug(s"Sending email, SendEmailRequest=$email")

    val futureResult = http.POST(emailUrl, email).map { response ⇒
      if (response.status >= 200 && response.status < 300)
        true
      else
        throw new BadGatewayException("Sending email is failed and it not queued for sending.")
    }

    futureResult.onComplete {
      case Success(_) ⇒
        Logger.info(s"Email sent for registration number ${userData.submissionReference}")
      case Failure(t) ⇒
        Logger.error(s"Email failure for registration number ${userData.submissionReference}", t)
    }
    futureResult
  }

}