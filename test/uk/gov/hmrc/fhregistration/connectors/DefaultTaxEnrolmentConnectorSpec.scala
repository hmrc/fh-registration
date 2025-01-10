/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito._
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class DefaultTaxEnrolmentConnectorSpec extends PlaySpec with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  "DefaultTaxEnrolmentConnector" should {
    "subscribe successfully" in {
      val serviceBaseUrl = "http://test/tax-enrolments"
      val callbackBase = "http://test/callback"
      val safeId = "safe123"
      val etmpFormBundleNumber = "bundle456"
      val requestBody = Json.obj(
        "serviceName" -> "HMRC-OBTDS-ORG",
        "callback"    -> s"$callbackBase/$etmpFormBundleNumber",
        "etmpId"      -> safeId
      )

      val mockResponse = mock(classOf[HttpResponse])
      val mockEnvironment = mock(classOf[Environment])
      val realConfiguration = Configuration(
        "microservice.services.tax-enrolments.host" -> "localhost",
        "microservice.services.tax-enrolments.port" -> "8080",
        "tax-enrolments.callback"                   -> "http://test/callback",
        "tax-enrolments.serviceName"                -> "HMRC-OBTDS-ORG"
      )
      val realEnvironment = Environment.simple()
      val mockHttpClient = mock(classOf[HttpClientV2])
      val mockRequestBuilder = mock(classOf[RequestBuilder])

      when(mockResponse.status).thenReturn(200)
      when(mockResponse.body).thenReturn("Success")
      when(mockEnvironment.mode).thenReturn(Mode.Test)

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsObject])(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val connector = new DefaultTaxEnrolmentConnector(mockHttpClient, realConfiguration, realEnvironment)

      connector.subscribe(safeId, etmpFormBundleNumber).map { result =>
        result mustBe mockResponse
        verify(mockHttpClient)
          .put(eqTo(new URL(s"$serviceBaseUrl/subscriptions/$etmpFormBundleNumber/subscriber")))(any[HeaderCarrier])
        verify(mockRequestBuilder).withBody(eqTo(requestBody))(any(), any(), any())
        verify(mockRequestBuilder).execute[HttpResponse](any(), any())
      }
    }

    "fail to subscribe with non-2xx response" in {
      val safeId = "safe123"
      val etmpFormBundleNumber = "bundle456"

      val realConfiguration = Configuration(
        "microservice.services.tax-enrolments.host" -> "localhost",
        "microservice.services.tax-enrolments.port" -> "8080",
        "tax-enrolments.callback"                   -> "http://test/callback",
        "tax-enrolments.serviceName"                -> "HMRC-OBTDS-ORG"
      )
      val realEnvironment = Environment.simple()
      val mockHttpClient = mock(classOf[HttpClientV2])
      val mockRequestBuilder = mock(classOf[RequestBuilder])
      val mockResponse = mock(classOf[HttpResponse])

      when(mockResponse.status).thenReturn(500)

      when(mockHttpClient.put(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any[JsObject])(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val connector = new DefaultTaxEnrolmentConnector(mockHttpClient, realConfiguration, realEnvironment)

      recoverToSucceededIf[RuntimeException] {
        connector.subscribe(safeId, etmpFormBundleNumber)
      }
    }

    "delete group enrolment successfully" in {
      val serviceBaseUrl = "http://test/tax-enrolments"
      val groupId = "group123"
      val registrationNumber = "reg456"

      val mockResponse = mock(classOf[HttpResponse])
      val mockEnvironment = mock(classOf[Environment])

      val realConfiguration = Configuration(
        "microservice.services.tax-enrolments.host" -> "localhost",
        "microservice.services.tax-enrolments.port" -> "8080",
        "tax-enrolments.callback"                   -> "http://test/callback",
        "tax-enrolments.serviceName"                -> "HMRC-OBTDS-ORG"
      )
      val realEnvironment = Environment.simple()

      when(mockResponse.status).thenReturn(200)
      when(mockResponse.body).thenReturn("Deleted")
      when(mockEnvironment.mode).thenReturn(Mode.Test)

      val mockHttpClient = mock(classOf[HttpClientV2])
      val mockRequestBuilder = mock(classOf[RequestBuilder])

      when(mockHttpClient.delete(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val connector = new DefaultTaxEnrolmentConnector(mockHttpClient, realConfiguration, realEnvironment)

      connector.deleteGroupEnrolment(groupId, registrationNumber).map { result =>
        result mustBe "Deleted"
        verify(mockHttpClient).delete(
          eqTo(
            new URL(
              s"$serviceBaseUrl/groups/$groupId/enrolments/HMRC-OBTDS-ORG~ETMPREGISTRATIONNUMBER~$registrationNumber"
            )
          )
        )(any[HeaderCarrier])
      }
    }

    "fail to delete group enrolment with non-2xx response" in {
      val serviceBaseUrl = "http://test/tax-enrolments"
      val groupId = "group123"
      val registrationNumber = "reg456"

      val mockResponse = mock(classOf[HttpResponse])
      val mockConfiguration = mock(classOf[Configuration])
      val mockEnvironment = mock(classOf[Environment])

      val realConfiguration = Configuration(
        "microservice.services.tax-enrolments.host" -> "localhost",
        "microservice.services.tax-enrolments.port" -> "8080",
        "tax-enrolments.callback"                   -> "http://test/callback",
        "tax-enrolments.serviceName"                -> "HMRC-OBTDS-ORG"
      )
      val realEnvironment = Environment.simple()

      when(mockResponse.status).thenReturn(500)
      when(mockConfiguration.get[String]("microservice.services.tax-enrolments.baseUrl"))
        .thenReturn(serviceBaseUrl)
      when(mockEnvironment.mode).thenReturn(Mode.Test)

      val mockHttpClient = mock(classOf[HttpClientV2])
      val mockRequestBuilder = mock(classOf[RequestBuilder])

      when(mockHttpClient.delete(any[URL])(any[HeaderCarrier])).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockResponse))

      val connector = new DefaultTaxEnrolmentConnector(mockHttpClient, realConfiguration, realEnvironment)

      recoverToSucceededIf[RuntimeException] {
        connector.deleteGroupEnrolment(groupId, registrationNumber)
      }
    }
  }
}
