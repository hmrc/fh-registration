package uk.gov.hmrc.fhregistration

import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData.{testEtmpFormBundleNumber, _}
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}

class FhddsApplicationAmendIntegrationSpecs extends TestHelpers with TestConfiguration {

  "Submit an amended application" should {
    "submit an amended application to DES, and get DES response" when {

      "the request has a valid amend payload" in {

        given().audit
          .writesAuditOrMerged()
          .des
          .acceptsAmendSubscription(testRegistrationNumber, testEtmpFormBundleNumber)
          .email
          .sendEmail

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

        given().audit
          .writesAuditOrMerged()
          .des
          .acceptsAmendSubscription(testRegistrationNumber, testEtmpFormBundleNumber)
          .email
          .sendEmail

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
