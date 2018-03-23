package uk.gov.hmrc.fhregistration

import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.{TestConfigures, TestHelpers}

class FhddsApplicationWithdrawalsIntegrationSpecs
  extends TestHelpers with TestConfigures {

  "Submit a withdrawal application" should {

    "submit a withdrawal application to DES, and get DES withdrawal response" when {

      "the request is a valid withdrawal request and Des returns right response" in {

        given().audit.writesAuditOrMerged()
          .des.acceptsWithdrawal(testRegistrationNumber)
          .email.sendEmail

        val responseForWithdrawal = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .withHeaders("Content-Type" -> "application/json")
            .post(testWithdrawalBody).futureValue
        }
        responseForWithdrawal.status shouldBe 200
      }

      "the request is an invalid withdrawal request" in {

        given().audit.writesAuditOrMerged()
          .des.acceptsWithdrawal(testRegistrationNumber)
          .email.sendEmail

        val responseForWithdrawal = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .withHeaders("Content-Type" -> "application/json")
            .post(testInvalidWithdrawalBody).futureValue
        }
        responseForWithdrawal.status shouldBe 400

      }

    }

  }

}
