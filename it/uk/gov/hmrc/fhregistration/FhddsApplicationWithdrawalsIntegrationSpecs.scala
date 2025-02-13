package uk.gov.hmrc.fhregistration

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData.*
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import uk.gov.hmrc.fhdds.testsupport.preconditions.DesStub

import scala.concurrent.Future

class FhddsApplicationWithdrawalsIntegrationSpecs extends TestHelpers with TestConfiguration with MockitoSugar {
  val mockDesStub = mock[DesStub]

  "Submitting an withdrawal request" should {
    "return BadRequest" when {
      "the request is invalid" in {
        
        when(mockDesStub.acceptsWithdrawal(testRegistrationNumber)).thenReturn(Future.successful("status"))
        val responseForWithdrawal = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .addHttpHeaders("Content-Type" -> "application/json")
            .addHttpHeaders("Authorization" -> "Bearer token")
            .post(testInvalidWithdrawalBody)
            .futureValue
        }
        responseForWithdrawal.status shouldBe 400
      }
    }

    "return BadRequest" when {
      "the user does not belong to a group" in {

        when(mockDesStub.acceptsWithdrawal(testRegistrationNumber)).thenReturn(Future.successful("status"))

        val responseForWithdrawal = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .addHttpHeaders("Content-Type" -> "application/json")
            .addHttpHeaders("Authorization" -> "Bearer token")
            .post(testWithdrawalBody)
            .futureValue
        }
        responseForWithdrawal.status shouldBe 400
      }
    }

    "return OK" when {
      "the user belongs to group" in {

        when(mockDesStub.acceptsWithdrawal(testRegistrationNumber)).thenReturn(Future.successful("status"))

        val responseForWithdrawal = WsTestClient.withClient { client =>
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .addHttpHeaders("Content-Type" -> "application/json")
            .addHttpHeaders("Authorization" -> "Bearer token")
            .post(testWithdrawalBody)
            .futureValue
        }
        responseForWithdrawal.status shouldBe 200

        expect().des
          .withdrawalCalled(testRegistrationNumber)
          .email
          .emailSent("fhdds_submission_withdrawal")
          .taxEnrolments
          .noDeEnrolmentCalled()

      }
    }
  }

}
