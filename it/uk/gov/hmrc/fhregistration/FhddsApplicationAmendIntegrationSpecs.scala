package uk.gov.hmrc.fhregistration

import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.{TestConfigures, TestHelpers}

class FhddsApplicationAmendIntegrationSpecs
  extends TestHelpers with TestConfigures {

  "Submit an amended application" should {
    "submit an amended application to DES, and get DES response" when {

      "some business registration details was saved" in {

        given().audit.writesAuditOrMerged()

        val responseForSaveRegistrationDetails = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/submission-extra-data/$testUserId/$testFormTypeRef/businessRegistrationDetails")
            .withHeaders("Content-Type" -> "application/json")
            .put(fakeBusinessDetailsJson).futureValue
        }
        responseForSaveRegistrationDetails.status shouldBe 202
      }

      "the business registration details has formID" in {

        given().audit.writesAuditOrMerged()

        val responseForUpdateFormId = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/submission-extra-data/$testUserId/$testFormTypeRef/formId")
            .put(Json.toJson(testFormId)).futureValue
        }
        responseForUpdateFormId.status shouldBe 202
      }

      "get DES response" in {

        val etmpFormBundleNumber = Array.fill(9)((math.random * 10).toInt).mkString

        given()
          .audit.writesAuditOrMerged()
          .des.acceptsSubscription(testSafeId, testRegistrationNumber, etmpFormBundleNumber)
          .taxEnrolment.subscribe
          .email.sendEmail

        WsTestClient.withClient { client ⇒
          whenReady(
            client
              .url(s"http://localhost:$port/fhdds/subscription/subscribe/$testRegistrationNumber")
              .withHeaders("Content-Type" -> "application/json")
              .post(Json.toJson(aSubmissionRequest)))
          { result ⇒
            result.status shouldBe 200
          }
        }

        expect()
          .des.verifiesSubscriptions()
      }
    }
  }

}
