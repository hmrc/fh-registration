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
import play.api.Logger
import play.api.mvc.Request
import uk.gov.hmrc.fhregistration.config.WSHttp
import uk.gov.hmrc.fhregistration.models.fhdds.UserData
import uk.gov.hmrc.fhregistration.models.fhdds.SendEmailRequest
import uk.gov.hmrc.fhregistration.services.{AuditService, AuditServiceImpl}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class EmailConnectorImpl extends EmailConnector with ServicesConfig {
  override val httpPost = WSHttp
  override val auditService = new AuditServiceImpl
  override val emailUrl = baseUrl("email") + "/hmrc/email"
  override val defaultEmailTemplateID =  getConfString(s"email.defaultTemplateId", "fhdds_submission_confirmation")
}

@ImplementedBy(classOf[EmailConnectorImpl])
trait EmailConnector {
  val httpPost: HttpPost
  
  val auditService: AuditService
  val emailUrl: String
  val defaultEmailTemplateID: String

  def sendEmail(emailTemplateId:String, userData: UserData)(implicit hc: HeaderCarrier, request: Request[AnyRef], ec: ExecutionContext): Future[Any] = {
    val toList: List[String] = List(userData.email)

    val email: SendEmailRequest = SendEmailRequest(templateId = emailTemplateId, to = toList, force = true)

    Logger.debug(s"Sending email, SendEmailRequest=$email")

    val futureResult = httpPost.POST(emailUrl, email).map { response ⇒
      if (response.status >= 200 && response.status < 300)
        true
      else
        throw new BadGatewayException("Sending email is failed and it not queued for sending.")
    }

    futureResult.onComplete {
      case Success(_) ⇒
        Logger.info(s"Email sent for registration number ${userData.submissionReference}")
        auditService.sendEmailSuccessEvent(userData)
      case Failure(t) ⇒
        Logger.error(s"Email failure for registration number ${userData.submissionReference}", t)
        auditService.sendEmailFailureEvent(userData, t)
    }
    futureResult
  }

}