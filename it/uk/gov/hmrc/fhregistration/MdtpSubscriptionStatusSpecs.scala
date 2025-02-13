package uk.gov.hmrc.fhregistration

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.preconditions.{AuditStub, DesStub}
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}
import uk.gov.hmrc.fhregistration.services.AuditService

import scala.concurrent.Future

class MdtpSubscriptionStatusSpecs extends TestHelpers with TestConfiguration with MockitoSugar {

  val receivedRegistrationNumber = "receivedRegistrationNumber"
  val processingRegistrationNumber = "processingRegistrationNumber"
  val successfulRegistrationNumber = "successfulRegistrationNumber"

  val mockAuditStub = mock[AuditStub]
  val mockDesStub = mock[DesStub]

  "Retrieve an mdtp subscription status from DES" should {

    "When bta calls fh-registration to retrieve a status and gets a 200 response with status" when {
      "if the the form is received" in {
        when(mockAuditStub.writesAuditOrMerged()).thenReturn(mockAuditStub)
        when(mockDesStub.getStatus(s"$receivedRegistrationNumber")).thenReturn(Future.successful("received"))

        val response = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/$receivedRegistrationNumber/status")
            .get()
            .futureValue
        }
        response.status shouldBe 200
        response.json.as[String] shouldBe "received"
      }

      "if the the form is processing" in {
        when(mockAuditStub.writesAuditOrMerged()).thenReturn(mockAuditStub)
        when(mockDesStub.getStatus(s"$processingRegistrationNumber")).thenReturn(Future.successful("processing"))

        val response = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/$processingRegistrationNumber/status")
            .get()
            .futureValue
        }
        response.status shouldBe 200
        response.json.as[String] shouldBe "processing"
      }

      "if the the form is successful" in {

        when(mockDesStub.getStatus(s"$successfulRegistrationNumber")).thenReturn(Future.successful("approved"))

        val response = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/$successfulRegistrationNumber/status")
            .get()
            .futureValue
        }
        response.status shouldBe 200
        response.json.as[String] shouldBe "approved"
      }

    }
  }

}
