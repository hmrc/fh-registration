/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.fhregistration.models.fhdds.{SendEmailRequest, UserData}
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpReads, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class EmailConnectorSpec extends HttpClientV2Helper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val request: Request[AnyRef] = FakeRequest()

  "EmailConnector" should {
    "send email successfully" in {

      val emailUrl = "http://test/email"
      val userData = UserData("test@example.com")
      val emailTemplateId = "testTemplateId"
      val emailParams = Map("key" -> "value")
      val sendEmailRequest = SendEmailRequest(List(userData.email), emailTemplateId, emailParams, force = true)

      val mockResponse = mock[HttpResponse]
      val mockConfiguration = mock[Configuration]
      val mockEnvironment = mock[Environment]

      val realConfiguration = Configuration(
        "microservice.services.email.host" -> "http://test",
        "microservice.services.email.port" -> "8080",
        "email.defaultTemplateId"          -> "defaultTemplate",
        "email.withdrawalEmailTemplateID"  -> "withdrawalTemplate",
        "email.deregisterEmailTemplateID"  -> "deregisterTemplate"
      )
      val realEnvironment = Environment.simple()

      when(mockConfiguration.getOptional[String](eqTo("email.defaultTemplateId"))(any()))
        .thenReturn(Some("defaultTemplate"))
      when(mockConfiguration.getOptional[String](eqTo("email.withdrawalEmailTemplateID"))(any()))
        .thenReturn(Some("withdrawalTemplate"))
      when(mockConfiguration.getOptional[String](eqTo("email.deregisterEmailTemplateID"))(any()))
        .thenReturn(Some("deregisterTemplate"))

      when(mockEnvironment.mode).thenReturn(Mode.Test)

      when(mockResponse.status).thenReturn(200)

      requestBuilderExecute(Future.successful(mockResponse))

      when(mockResponse.status).thenReturn(200)
      requestBuilderExecute(Future.successful(mockResponse))

      val emailConnector = new DefaultEmailConnector(mockHttp, realConfiguration, realEnvironment)

      emailConnector
        .sendEmail(emailTemplateId, userData, emailParams)
        .map { result =>
          result shouldBe true
          verify(requestBuilder).withBody(eqTo(Json.toJson(sendEmailRequest)))(any(), any(), any())
          verify(requestBuilder).execute[HttpResponse](any[HttpReads[HttpResponse]], any[ExecutionContext])
        }
        .map(_ => succeed)
    }

    "fail to send email" in {
      val emailUrl = "http://test/email"
      val userData = UserData("test@example.com")
      val emailTemplateId = "testTemplateId"
      val emailParams = Map("key" -> "value")

      val mockResponse = mock[HttpResponse]
      val mockConfiguration = mock[Configuration]
      val mockEnvironment = mock[Environment]

      val realConfiguration = Configuration(
        "microservice.services.email.host" -> "http://test",
        "microservice.services.email.port" -> "8080",
        "email.defaultTemplateId"          -> "defaultTemplate",
        "email.withdrawalEmailTemplateID"  -> "withdrawalTemplate",
        "email.deregisterEmailTemplateID"  -> "deregisterTemplate"
      )
      val realEnvironment = Environment.simple()

      when(mockConfiguration.getOptional[String](eqTo("email.defaultTemplateId"))(any()))
        .thenReturn(Some("defaultTemplate"))
      when(mockConfiguration.getOptional[String](eqTo("email.withdrawalEmailTemplateID"))(any()))
        .thenReturn(Some("withdrawalTemplate"))
      when(mockConfiguration.getOptional[String](eqTo("email.deregisterEmailTemplateID"))(any()))
        .thenReturn(Some("deregisterTemplate"))

      when(mockEnvironment.mode).thenReturn(Mode.Test)

      when(mockResponse.status).thenReturn(500)

      requestBuilderExecute(Future.successful(mockResponse))

      val emailConnector = new DefaultEmailConnector(mockHttp, realConfiguration, realEnvironment)

      recoverToSucceededIf[BadGatewayException] {
        emailConnector.sendEmail(emailTemplateId, userData, emailParams)
      }
    }
  }
}
