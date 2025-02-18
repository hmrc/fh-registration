package uk.gov.hmrc.fhregistration

import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData.*
import uk.gov.hmrc.fhdds.testsupport.preconditions.{AuditStub, DesStub, EmailStub, TaxEnrolmentStub, UserStub}
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}
import uk.gov.hmrc.fhregistration.models.TaxEnrolmentsCallback
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress.EnrolmentProgress
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

class FhddsApplicationIntegrationSpecs
    extends TestHelpers with TestConfiguration with MockitoSugar with AnyWordSpecLike {

  val auditMock = mock[AuditStub]
  val desMock = mock[DesStub]
  val taxEnrolmentMock = mock[TaxEnrolmentStub]
  val emailMock = mock[EmailStub]
  val userMock = mock[UserStub]

  "Submit an application" should {

    "submit an application to DES, and get DES response" when {

      "the request has a valid application payload" in {

        when(auditMock.writesAuditOrMerged()).thenReturn(auditMock)
        when(desMock.acceptsSubscription(testSafeId, testRegistrationNumber, testEtmpFormBundleNumber))
          .thenReturn(desMock)
        when(taxEnrolmentMock.subscribe).thenReturn(taxEnrolmentMock)
        when(emailMock.sendEmail).thenReturn(emailMock)
        when(userMock.isAuthorised()).thenReturn(userMock)

        val result = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/subscribe/$testSafeId")
            .addHttpHeaders("Content-Type" -> "application/json")
            .addHttpHeaders("Authorization" -> "Bearer token")
            .post(Json.toJson(validSubmissionRequest))
            .futureValue
        }

        result.status shouldBe 200

        expect().des
          .verifiesSubscriptions()
          .taxEnrolments
          .enrolmentCalled(testRegistrationNumber, testEtmpFormBundleNumber)

        enrolmentProgress shouldBe EnrolmentProgress.Pending

        makeTaxEnrolmentsCallback

        enrolmentProgress shouldBe EnrolmentProgress.Unknown

        expect().email.emailSent()

      }

      "the request without a valid application payload" in {

        when(auditMock.writesAuditOrMerged()).thenReturn(auditMock)
        when(desMock.acceptsSubscription(testSafeId, testRegistrationNumber, testEtmpFormBundleNumber))
          .thenReturn(desMock)
        when(taxEnrolmentMock.subscribe).thenReturn(taxEnrolmentMock)
        when(emailMock.sendEmail).thenReturn(emailMock)

        val result = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/subscribe/$testSafeId")
            .addHttpHeaders("Content-Type" -> "application/json")
            .post(Json.toJson(""))
            .futureValue
        }

        result.status shouldBe 400

      }
    }

    "Submit an application then remove the previous enrolment on callback" in {

      when(auditMock.writesAuditOrMerged()).thenReturn(auditMock)
      when(desMock.acceptsSubscription(testSafeId, anotherRegistrationNumber, testEtmpFormBundleNumber))
        .thenReturn(desMock)
      when(taxEnrolmentMock.subscribe).thenReturn(taxEnrolmentMock)
      when(taxEnrolmentMock.acceptsDeEnrolment()).thenReturn(taxEnrolmentMock)
      when(emailMock.sendEmail).thenReturn(emailMock)
      when(userMock.isAuthorised()).thenReturn(userMock)

      val result = WsTestClient.withClient { client =>
        client
          .url(
            s"http://localhost:$port/fhdds/subscription/subscribe/$testSafeId?currentRegNumber=$testRegistrationNumber"
          )
          .addHttpHeaders("Content-Type" -> "application/json")
          .addHttpHeaders("Authorization" -> "Bearer token")
          .post(Json.toJson(validSubmissionRequest))
          .futureValue
      }

      result.status shouldBe 200

      enrolmentProgress shouldBe EnrolmentProgress.Pending

      expect().des
        .verifiesSubscriptions()
        .taxEnrolments
        .enrolmentCalled(anotherRegistrationNumber, testEtmpFormBundleNumber)
        .taxEnrolments
        .deEnrolmentCalled("some-group", testRegistrationNumber)

      makeTaxEnrolmentsCallback

      enrolmentProgress shouldBe EnrolmentProgress.Unknown

    }

  }

  private def makeTaxEnrolmentsCallback = {
    val taxEnrolmentsCallback = TaxEnrolmentsCallback("", "SUCCEEDED", None)
    val callbackResult = WsTestClient.withClient { client =>
      client
        .url(s"http://localhost:$port/fhdds/tax-enrolment/callback/subscriptions/$testEtmpFormBundleNumber")
        .post(Json.toJson(taxEnrolmentsCallback))
        .futureValue
    }

    callbackResult.status shouldBe 200
  }

  private def enrolmentProgress = {
    val pendingResult = WsTestClient.withClient { client =>
      client
        .url(s"http://localhost:$port/fhdds/subscription/enrolmentProgress")
        .addHttpHeaders("Authorization" -> "Bearer token")
        .get()
        .futureValue
    }
    pendingResult.json.as[EnrolmentProgress]
  }
}
