package uk.gov.hmrc.fhregistration

import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}

class FhddsApplicationIntegrationSpecs
  extends TestHelpers with TestConfiguration {

  "Submit an application" should {
    "submit an application to DES, and get DES response" when {

      "the request has a valid application payload" in {

        given()
          .audit.writesAuditOrMerged()
          .des.acceptsSubscription(testSafeId, testRegistrationNumber, testEtmpFormBundleNumber)
          .taxEnrolment.subscribe
          .email.sendEmail
          .user.isAuthorised()

        WsTestClient.withClient { client ⇒
          whenReady(
            client
              .url(s"http://localhost:$port/fhdds/subscription/subscribe/$testSafeId")
              .withHeaders("Content-Type" -> "application/json")
              .post(Json.toJson(validSubmissionRequest))) { result ⇒
            result.status shouldBe 200
          }
        }

        expect()
          .des.verifiesSubscriptions()
      }

      "the request without a valid application payload" in {


        val registrationNumber = Array.fill(9)((math.random * 10).toInt).mkString
        val etmpFormBundleNumber = Array.fill(9)((math.random * 10).toInt).mkString
        given()
          .audit.writesAuditOrMerged()
          .des.acceptsSubscription(testSafeId, testRegistrationNumber, testEtmpFormBundleNumber)
          .taxEnrolment.subscribe
          .email.sendEmail

        WsTestClient.withClient { client ⇒
          whenReady(
            client
              .url(s"http://localhost:$port/fhdds/subscription/subscribe/$testSafeId")
              .withHeaders("Content-Type" -> "application/json")
              .post(Json.toJson(""))) { result ⇒
            result.status shouldBe 400
          }
        }


      }
    }

  }
}
