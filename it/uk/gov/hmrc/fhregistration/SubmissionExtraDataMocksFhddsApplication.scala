package uk.gov.hmrc.fhregistration

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpec, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.TestedApplication
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails


class SubmissionExtraDataMocksFhddsApplication
  extends WordSpec
    with OptionValues
    with WsScalaTestClient
    with TestedApplication
    with WordSpecLike
    with Matchers
    with ScalaFutures {

  "SubmissionExtraDataController" should {
    "save and later retrieve a bussiness registration detail" when {
      "save details with a userId and formTypeRef" in {

        given().audit.writesAuditOrMerged()

        val response = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/submission-extra-data/$testUserId/$testFormTypeRef/businessRegistrationDetails")
            .withHeaders("Content-Type" -> "application/json")
            .put(fakeBusinessDetailsJson).futureValue
        }
        response.status shouldBe 202
      }

      "retrieve details with valid userId and formTypeRef" in {

        given().audit.writesAuditOrMerged()

        val response = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/submission-extra-data/$testUserId/$testFormTypeRef/businessRegistrationDetails")
            .get().futureValue
        }
        response.status shouldBe 200
        response.json.validate[BusinessRegistrationDetails].isSuccess shouldBe true
        response.json shouldBe Json.parse(fakeBusinessDetailsJson)
      }

      "retrieve details without valid userId and formTypeRef" in {

        given().audit.writesAuditOrMerged()

        val response = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/submission-extra-data/$invalidUserId/$invalidFormTypeRef/businessRegistrationDetails")
            .get().futureValue
        }
        response.status shouldBe 404
      }

    }
  }

}
