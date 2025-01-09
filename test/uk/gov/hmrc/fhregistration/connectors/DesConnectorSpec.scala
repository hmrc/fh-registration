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

import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.fhregistration.models.des.{DesStatus, StatusResponse}
import uk.gov.hmrc.fhregistration.util.UnitSpec
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DesConnectorSpec extends UnitSpec with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  class DefaultDesConnectorMock(
    val httpClient: HttpClientV2,
    val config: Configuration,
    environment: Environment,
    servicesConfig: ServicesConfig
  ) extends DefaultDesConnector(httpClient, config, environment, servicesConfig) {
    override def baseUrl(serviceName: scala.Predef.String): scala.Predef.String = serviceName
    override def config(serviceName: scala.Predef.String): Configuration = new Configuration(mock[Config])
  }

  "URLs and URIs" should {
    val desConnectorMock =
      new DefaultDesConnectorMock(mock[HttpClientV2], mock[Configuration], mock[Environment], mock[ServicesConfig])

    "desServiceStatusUri" in {
      desConnectorMock.desServiceStatusUri shouldBe "des-service"
    }

    "desSubmissionUrl" in {
      desConnectorMock.desSubmissionUrl("testValue") shouldBe "des-service/id/testValue/id-type/safe"
    }

    "desAmendmentUrl" in {
      desConnectorMock.desAmendmentUrl("testValue") shouldBe "des-service/id/testValue/id-type/fhdds"
    }

    "desWithdrawalUrl" in {
      desConnectorMock.desWithdrawalUrl("testValue") shouldBe "des-service/testValue/withdrawal"
    }

    "desDeregisterUrl" in {
      desConnectorMock.desDeregisterUrl("testValue") shouldBe "des-service/testValue/deregistration"
    }
  }

  "customDesRead" should {
    val desConnectorMock =
      new DefaultDesConnectorMock(mock[HttpClientV2], mock[Configuration], mock[Environment], mock[ServicesConfig])

    "successfully convert 429 from DES to 503" in {
      val httpResponse = HttpResponse(429, "429")
      val ex = intercept[UpstreamErrorResponse](desConnectorMock.customDESRead(httpResponse))
      ex shouldBe UpstreamErrorResponse("429 received from DES - converted to 503", 429, 503)
    }
  }

  "DefaultDesConnector" should {
    val mockHttpClient = mock[HttpClientV2]
    val mockConfig = mock[Configuration]
    val mockEnvironment = mock[Environment]
    val mockServicesConfig = mock[ServicesConfig]
    val desConnectorMock = new DefaultDesConnectorMock(mockHttpClient, mockConfig, mockEnvironment, mockServicesConfig)

    "construct URLs and URIs correctly" should {
      "desServiceStatusUri" in {
        desConnectorMock.desServiceStatusUri shouldBe "des-service"
      }

      "desSubmissionUrl" in {
        desConnectorMock.desSubmissionUrl("testValue") shouldBe "des-service/id/testValue/id-type/safe"
      }

      "desAmendmentUrl" in {
        desConnectorMock.desAmendmentUrl("testValue") shouldBe "des-service/id/testValue/id-type/fhdds"
      }

      "desWithdrawalUrl" in {
        desConnectorMock.desWithdrawalUrl("testValue") shouldBe "des-service/testValue/withdrawal"
      }

      "desDeregisterUrl" in {
        desConnectorMock.desDeregisterUrl("testValue") shouldBe "des-service/testValue/deregistration"
      }
    }

    "handle customDESRead correctly" should {
      "convert 429 to 503" in {
        val httpResponse = HttpResponse(429, "429")
        val ex = intercept[UpstreamErrorResponse](desConnectorMock.customDESRead(httpResponse))
        ex shouldBe UpstreamErrorResponse("429 received from DES - converted to 503", 429, 503)
      }

      "log and return 403 response" in {
        val httpResponse = HttpResponse(403, "403")
        val result = desConnectorMock.customDESRead(httpResponse)
        result shouldBe httpResponse
      }

      "pass through other statuses" in {
        val httpResponse = HttpResponse(200, "200")
        val result = desConnectorMock.customDESRead(httpResponse)
        result shouldBe httpResponse
      }
    }

    "perform getStatus" should {
      "handle successful response" in {
        val mockRequestBuilder = mock[RequestBuilder]
        val mockHttpResponse = HttpResponse(
          200,
          Json
            .obj(
              "subscriptionStatus" -> "Successful",
              "idType"             -> "idTypeValue",
              "idValue"            -> "idValueValue"
            )
            .toString()
        )

        when(mockHttpClient.get(any())(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        val realConfiguration = Configuration.from(
          Map(
            "microservice.services.des-service.host"                -> "localhost",
            "microservice.services.des-service.port"                -> "8080",
            "microservice.services.des-service.authorization-token" -> "test-token",
            "microservice.services.des-service.environment"         -> "test-environment"
          )
        )
        val realEnvironment = Environment.simple()

        val desConnectorMock =
          new DefaultDesConnector(mockHttpClient, realConfiguration, realEnvironment, mock[ServicesConfig]) {
            override def baseUrl(serviceName: String): String = "http://localhost:8080"
          }

        val result = desConnectorMock.getStatus("fhdds123")(HeaderCarrier()).futureValue

        result shouldBe StatusResponse(
          subscriptionStatus = DesStatus.Successful,
          idType = Some("idTypeValue"),
          idValue = Some("idValueValue")
        )
      }
    }

    "perform sendSubmission" should {
      "handle successful submission" in {
        val submission = Json.obj("key" -> "value")
        val httpResponse = HttpResponse(
          200,
          Json
            .obj(
              "registrationNumberFHDDS" -> "123",
              "processingDate"          -> "2023-12-01",
              "etmpFormBundleNumber"    -> "bundle456"
            )
            .toString()
        )
        val mockRequestBuilder = mock[RequestBuilder]

        when(mockHttpClient.post(any())(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody[JsValue](any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val realConfiguration = Configuration.from(
          Map(
            "microservice.services.des-service.host"                -> "localhost",
            "microservice.services.des-service.port"                -> "8080",
            "microservice.services.des-service.authorization-token" -> "test-token",
            "microservice.services.des-service.environment"         -> "test-environment"
          )
        )
        val realEnvironment = Environment.simple()

        val desConnectorMock =
          new DefaultDesConnector(mockHttpClient, realConfiguration, realEnvironment, mock[ServicesConfig]) {
            override def baseUrl(serviceName: String): String = "http://localhost:8080"
          }

        val result = desConnectorMock.sendSubmission("safe123", submission)(HeaderCarrier()).futureValue

        result.registrationNumberFHDDS shouldBe "123"
        result.etmpFormBundleNumber shouldBe "bundle456"
      }
    }

    "perform sendAmendment" should {
      "handle successful sendAmendment" in {
        val submission = Json.obj("key" -> "value")
        val httpResponse = HttpResponse(
          200,
          Json
            .obj(
              "registrationNumberFHDDS" -> "123",
              "processingDate"          -> "2023-12-01",
              "etmpFormBundleNumber"    -> "bundle456"
            )
            .toString()
        )
        val mockRequestBuilder = mock[RequestBuilder]

        when(mockHttpClient.post(any())(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody[JsValue](any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val realConfiguration = Configuration.from(
          Map(
            "microservice.services.des-service.host"                -> "localhost",
            "microservice.services.des-service.port"                -> "8080",
            "microservice.services.des-service.authorization-token" -> "test-token",
            "microservice.services.des-service.environment"         -> "test-environment"
          )
        )
        val realEnvironment = Environment.simple()

        val desConnectorMock =
          new DefaultDesConnector(mockHttpClient, realConfiguration, realEnvironment, mock[ServicesConfig]) {
            override def baseUrl(serviceName: String): String = "http://localhost:8080"
          }

        val result = desConnectorMock.sendAmendment("safe123", submission)(HeaderCarrier()).futureValue

        result.registrationNumberFHDDS shouldBe "123"
        result.etmpFormBundleNumber shouldBe "bundle456"
      }
    }

    "perform display" should {
      "handle successful response" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockRequestBuilder = mock[RequestBuilder]
        val mockHttpResponse = HttpResponse(
          200,
          Json
            .obj(
              "subscriptionStatus" -> "Active",
              "idType"             -> "idTypeValue",
              "idValue"            -> "idValueValue"
            )
            .toString()
        )

        val realConfiguration = Configuration.from(
          Map(
            "microservice.services.des-service.host"                -> "localhost",
            "microservice.services.des-service.port"                -> "8080",
            "microservice.services.des-service.authorization-token" -> "test-token",
            "microservice.services.des-service.environment"         -> "test-environment"
          )
        )
        val realEnvironment = Environment.simple()

        val desConnectorMock =
          new DefaultDesConnector(mockHttpClient, realConfiguration, realEnvironment, mock[ServicesConfig]) {
            override def baseUrl(serviceName: String): String = "http://localhost:8080"
          }

        when(mockHttpClient.get(any())(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        val result = desConnectorMock.display("fhdds123")(HeaderCarrier()).futureValue
        result.status shouldBe 200
      }
    }

  }
}
