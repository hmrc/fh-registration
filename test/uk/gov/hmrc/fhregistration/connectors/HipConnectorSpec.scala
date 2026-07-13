/*
 * Copyright 2026 HM Revenue & Customs
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

import ch.qos.logback.classic.{Level, Logger as LogbackLogger}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, contains}
import org.mockito.Mockito.{verify, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import sttp.model.HeaderNames
import uk.gov.hmrc.fhregistration.util.LogCapturing
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.text.SimpleDateFormat
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class HipConnectorSpec extends AnyWordSpecLike with Matchers with OptionValues with MockitoSugar with LogCapturing {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val configuration = Configuration.from(
    Map(
      "microservice.services.hip.host"         -> "localhost",
      "microservice.services.hip.port"         -> "1120",
      "microservice.services.hip.clientId"     -> "testId",
      "microservice.services.hip.clientSecret" -> "testSecret"
    )
  )

  val mockHttpClient = mock[HttpClientV2]


  class DefaultHipConnectorMock(
    val httpClient: HttpClientV2,
    val config: Configuration
  ) extends DefaultHipConnector(httpClient, config) {
    def underlyingLogger: LogbackLogger = logger.logger.asInstanceOf[LogbackLogger]
  }

  val fhddsRegistrationNumber = "XAFH00000123456"

  "hipServiceBasePath" should {
    val hipConnectorMock = new DefaultHipConnectorMock(mock[HttpClientV2], configuration)

    "have correct server address and base path" in {
      hipConnectorMock.hipServiceBasePath shouldBe "http://localhost:1120/etmp/RESTAdapter/fulfilment-diligence/subscription"
    }

  }

  "subscription" should {
    val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)


    "have correct SubscriptionDisplay path" in {
      val result = connector.subscriptionDisplayUrl(fhddsRegistrationNumber)
      result shouldBe s"http://localhost:1120/etmp/RESTAdapter/fulfilment-diligence/subscription/$fhddsRegistrationNumber"
    }

    "have correct SubscriptionWithdrawal path" in{
      val result = connector.subscriptionWithdrawalUrl(fhddsRegistrationNumber)
      result shouldBe s"http://localhost:1120/etmp/RESTAdapter/fulfilment-diligence/subscription/withdrawal/$fhddsRegistrationNumber"
    }

    "have correct CreateAndUpdate path" in {
      val idType = "fhdds"
      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.createOrUpdateFhddsUrl(fhddsRegistrationNumber, idType)
      result shouldBe s"http://localhost:1120/etmp/RESTAdapter/fulfilment-diligence/subscription/id/$fhddsRegistrationNumber/id-type/$idType"
    }

    "have correct SubscriptionDeRegister path" in {
      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.subscriptionDeregistrationUrl(fhddsRegistrationNumber)
      result shouldBe s"http://localhost:1120/etmp/RESTAdapter/fulfilment-diligence/subscription/deregistration/$fhddsRegistrationNumber"
    }


  }

  "subscriptionDisplay" should {

    "return the HIP response" in {
      val mockRequestBuilder = mock[RequestBuilder]
      val jsonBody = Source.fromResource("json/valid/subscription/fhdds-display-response.json").mkString
      val httpResponse = HttpResponse(200, jsonBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

      result.status shouldBe 200
      result.body shouldBe jsonBody

      val headersCaptor: ArgumentCaptor[Seq[(String, String)]] =
        ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      verify(mockRequestBuilder).setHeader(headersCaptor.capture() *)
      val headers = headersCaptor.getValue.toMap
      headers.keySet shouldBe Set(
        "correlationid",
        HeaderNames.Authorization,
        "X-Originating-System",
        "X-Receipt-Date",
        "X-Transmitting-System"
      )
    }

    "return BadRequest from HOD(SystemError) and log error" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody = """{
                           |  "origin": "HoD",
                           |  "response": {
                           |    "error": {
                           |      "code": "400",
                           |      "message": "string",
                           |      "logID": "D82EBAB67AC6D7565C0682CA91BDC577"
                           |    }
                           |  }
                           |}""".stripMargin

      val httpResponse = HttpResponse(400, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 400
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 400 from HIP with message - $responseBody"
        )
      }
    }

    "return BadRequest from HIP and log error" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody =
        """{
          |  "failures": [
          |    {
          |      "type": "bad request",
          |      "reason": "fhdds number is invalid."
          |    }
          |  ]
          |}""".stripMargin

      val httpResponse = HttpResponse(400, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 400
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 400 from HIP with message - $responseBody"
        )
      }
    }

    "return Unauthorized and log accordingly" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody = ""

      val httpResponse = HttpResponse(401, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 401
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 401 from HIP with message - $responseBody"
        )
      }
    }

    "return Forbidden and log accordingly" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody = ""

      val httpResponse = HttpResponse(403, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 403
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 403 from HIP with message - $responseBody"
        )
      }
    }

    "return NotFound and log accordingly" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody = ""

      val httpResponse = HttpResponse(404, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 404
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 404 from HIP with message - $responseBody"
        )
      }
    }

    "return 422 and log accordingly" in {
      val mockRequestBuilder = mock[RequestBuilder]
      val responseBody = """[{"code":"002", "text":"ID not found"}]"""
      val httpResponse = HttpResponse(422, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 422
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 422 from HIP with message - $responseBody"
        )
      }
    }

    "return 500 from HIP and log error" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody =
        """{
          |  "error": {
          |    "code": "500",
          |    "message": "Server error",
          |    "logID": "123456789"
          |  }
          |}""".stripMargin

      val httpResponse = HttpResponse(500, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 500
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 500 from HIP with message - $responseBody"
        )
      }
    }

    "return 503 from HIP and log error" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody =
        """{
          |  "origin": "HIP",
          |  "response": {
          |    "failures": [
          |      {
          |        "type": "Unavailable",
          |        "reason": "unknown"
          |      }
          |    ]
          |  }
          |}""".stripMargin

      val httpResponse = HttpResponse(503, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 503
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 503 from HIP with message - $responseBody"
        )
      }
    }


  }

  "subscriptionWithdrawal" should {
    val mockRequestBuilder = mock[RequestBuilder]

    "return the HIP response" in {
      val payload = """{
                      |  "withdrawalDate": "2015-08-23",
                      |  "withdrawalReason": "Other",
                      |  "withdrawalReasonOther": "Other Reason"
                      |}""".stripMargin

      val jsonBody = """{
                       |  "success": {
                       |    "processingDate": "2001-12-17T09:30:47Z"
                       |  }
                       |}""".stripMargin
      val httpResponse = HttpResponse(200, jsonBody)

      when(mockHttpClient.put(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody[JsValue](any())(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.subscriptionWithdrawal(fhddsRegistrationNumber, Json.parse(payload))(hc).futureValue

      result.processingDate shouldBe new SimpleDateFormat("yyyy-MM-dd").parse("2001-12-17")


      val headersCaptor: ArgumentCaptor[Seq[(String, String)]] =
        ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      verify(mockRequestBuilder).setHeader(headersCaptor.capture() *)
      val headers = headersCaptor.getValue.toMap
      headers.keySet shouldBe Set(
        "correlationid",
        HeaderNames.Authorization,
        "X-Originating-System",
        "X-Receipt-Date",
        "X-Transmitting-System"
      )
    }

    "return BadRequest from HIP and log error" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val responseBody =
        """{
          |  "failures": [
          |    {
          |      "type": "VALIDATION_ERROR",
          |      "reason": "The business registration number is invalid."
          |    }
          |  ]
          |}""".stripMargin

      val httpResponse = HttpResponse(400, responseBody)

      when(mockHttpClient.get(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)

      withCaptureOfLoggingFrom(connector.underlyingLogger) { logs =>
        val result = connector.subscriptionDisplay(fhddsRegistrationNumber)(hc).futureValue

        result.status shouldBe 400
        result.body shouldBe responseBody
        logs.map(_.getLevel) shouldBe List(Level.ERROR)
        logs.map(_.getFormattedMessage) shouldBe List(
          s"Received error 400 from HIP with message - $responseBody"
        )
      }
    }

  }

  "createOrUpdateFhdds" should {
    "return the HIP response for Create" in {
      val mockRequestBuilder = mock[RequestBuilder]

      val jsonBody =
        """{
          |  "success": {
          |    "processingDate": "2001-12-17T09:30:47Z",
          |    "etmpFormBundleNumber": "012345678901",
          |    "registrationNumberFHDDS": "XDFH00000123456"
          |  }
          |}""".stripMargin
      val httpResponse = HttpResponse(200, jsonBody)


      val submissionRequest = Source.fromResource("json/valid/subscription/fhdds-create.json").mkString


      when(mockHttpClient.post(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody[JsValue](any())(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.createOrUpdateFhdds("XA0001234567890", Json.parse(submissionRequest))(hc).futureValue

      result.processingDate shouldBe new SimpleDateFormat("yyyy-MM-dd").parse("2001-12-17")
      result.etmpFormBundleNumber shouldBe "012345678901"
      result.registrationNumberFHDDS shouldBe "XDFH00000123456"
    }

    "return the HIP response for Update" in {

      val mockRequestBuilder = mock[RequestBuilder]
      val jsonBody =
        """{
          |  "success": {
          |    "processingDate": "2001-12-17T09:30:47Z",
          |    "etmpFormBundleNumber": "012345678901",
          |    "registrationNumberFHDDS": "XDFH00000123456"
          |  }
          |}""".stripMargin
      val httpResponse = HttpResponse(200, jsonBody)


      val submissionRequest = Source.fromResource("json/valid/subscription/fhdds-update.json").mkString


      when(mockHttpClient.post(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody[JsValue](any())(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.createOrUpdateFhdds("XA0001234567890", Json.parse(submissionRequest))(hc).futureValue



      result.processingDate shouldBe new SimpleDateFormat("yyyy-MM-dd").parse("2001-12-17")
      result.etmpFormBundleNumber shouldBe "012345678901"
      result.registrationNumberFHDDS shouldBe "XDFH00000123456"

    }
  }

  "subscriptionDeregistration" should{

    "return the HIP response" in {
      val mockRequestBuilder = mock[RequestBuilder]
      val payload =
        """{
          |  "date": "2012-02-10",
          |  "reason": "Others",
          |  "reasonOther": "Text And Reason here"
          |}""".stripMargin

      val jsonBody =
        """{
          |  "success": {
          |    "processingDate": "2001-12-17T09:30:47Z"
          |  }
          |}""".stripMargin
      val httpResponse = HttpResponse(201, jsonBody)

      when(mockHttpClient.post(any())(using any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody[JsValue](any())(using any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](using any(), any())).thenReturn(Future.successful(httpResponse))

      val connector = new DefaultHipConnectorMock(mockHttpClient, configuration)
      val result = connector.subscriptionDeregistration(fhddsRegistrationNumber, Json.parse(payload))(hc).futureValue

      result.processingDate shouldBe new SimpleDateFormat("yyyy-MM-dd").parse("2001-12-17")


      val headersCaptor: ArgumentCaptor[Seq[(String, String)]] =
        ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
      verify(mockRequestBuilder).setHeader(headersCaptor.capture() *)
      val headers = headersCaptor.getValue.toMap
      headers.keySet shouldBe Set(
        "correlationid",
        HeaderNames.Authorization,
        "X-Originating-System",
        "X-Receipt-Date",
        "X-Transmitting-System"
      )

    }

  }



}
