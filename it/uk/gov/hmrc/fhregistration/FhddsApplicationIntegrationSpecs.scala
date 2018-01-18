package uk.gov.hmrc.fhregistration

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpec, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.TestedApplication

class FhddsApplicationIntegrationSpecs
  extends WordSpec
    with OptionValues
    with WsScalaTestClient
    with TestedApplication
    with WordSpecLike
    with Matchers
    with ScalaFutures {

  "Submit an application" should {
    "submit an application to DES, and get DES response" when {

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
        given()
          .audit.writesAuditOrMerged()
          .des.sendSubmission(testSafeId)
          .taxEnrolment.subscribe(testSafeId)

        WsTestClient.withClient { client ⇒
          whenReady(
            client
              .url(s"http://localhost:$port/fhdds/application/submit")
              .post(Json.toJson(aSubmissionRequest))) { result ⇒
            result.status shouldBe 200
          }
        }
      }
    }
  }

}
