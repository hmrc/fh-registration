package uk.gov.hmrc.fhregistration

import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}

class FhddsApplicationWithdrawalsIntegrationSpecs
  extends TestHelpers with TestConfiguration {

  "Submitting an withdrawal request" should {
    "return BadRequest" when {
      "the request is invalid" in {
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

    "return BadRequest" when {
      "the user does not belong to a group" in {

        given().audit.writesAuditOrMerged()
          .des.acceptsWithdrawal(testRegistrationNumber)
          .email.sendEmail
          .user.isAuthorisedWithNoGroup()

        val responseForWithdrawal = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .withHeaders("Content-Type" -> "application/json")
            .post(testWithdrawalBody).futureValue
        }
        responseForWithdrawal.status shouldBe 400
      }
    }

    "return OK" when {
      "the user belongs to group" in {
        given().audit.writesAuditOrMerged()
          .des.acceptsWithdrawal(testRegistrationNumber)
          .email.sendEmail
          .user.isAuthorised()
          .taxEnrolment.acceptsDeEnrolment()

        val responseForWithdrawal = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/subscription/withdrawal/$testRegistrationNumber")
            .withHeaders("Content-Type" -> "application/json")
            .post(testWithdrawalBody).futureValue
        }
        responseForWithdrawal.status shouldBe 200

        expect()
          .des.withdrawalCalled(testRegistrationNumber)
          .email.emailSent()
          .taxEnrolments.deEnrolmentCalled("some-group", testRegistrationNumber)


      }
    }
  }

}
