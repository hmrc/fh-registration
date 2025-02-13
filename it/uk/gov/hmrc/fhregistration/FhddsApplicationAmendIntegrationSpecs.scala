package uk.gov.hmrc.fhregistration

import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData.*
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.fhdds.testsupport.preconditions.{AuditStub, DesStub, EmailStub}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

class FhddsApplicationAmendIntegrationSpecs
    extends TestHelpers with TestConfiguration with MockitoSugar with ScalaFutures {

  val auditMock = mock[AuditStub]
  val desMock = mock[DesStub]
  val emailMock = mock[EmailStub]

  "Submit an amended application" should {
    "submit an amended application to DES, and get DES response" when {

      "the request has a valid amend payload" in {

        when(auditMock.writesAuditOrMerged()).thenReturn(auditMock)
        when(desMock.acceptsAmendSubscription(testRegistrationNumber, testEtmpFormBundleNumber)).thenReturn(desMock)
        when(emailMock.sendEmail).thenReturn(emailMock)

        WsTestClient.withClient { client =>
          whenReady(
            client
              .url(s"http://localhost:$port/fhdds/subscription/amend/$testRegistrationNumber")
              .addHttpHeaders("Content-Type" -> "application/json")
              .post(Json.toJson(validAmendSubmissionRequest))
          ) { result =>
            result.status shouldBe 200
          }
        }
        expect().des.verifiesSubscriptions()
      }

      "the request without a valid amend payload" in {

        when(auditMock.writesAuditOrMerged()).thenReturn(auditMock)
        when(desMock.acceptsAmendSubscription(testRegistrationNumber, testEtmpFormBundleNumber)).thenReturn(desMock)
        when(emailMock.sendEmail).thenReturn(emailMock)

        WsTestClient.withClient { client =>
          whenReady(
            client
              .url(s"http://localhost:$port/fhdds/subscription/amend/$testRegistrationNumber")
              .addHttpHeaders("Content-Type" -> "application/json")
              .post(Json.toJson(""))
          ) { result =>
            result.status shouldBe 400
          }
        }
      }

    }

  }

}
