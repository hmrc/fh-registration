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

        <<<<<<< HEAD
          //  feature("Get Business Registration Details") {
          //
          //    scenario("With valid or not valid userId and formTypeRef") {
          //
          //      info("Save a business registration retails with userId and formTypeRef")
          //      submissionExtraDataController.saveBusinessRegistrationDetails(testUserId, testFormTypeRef)
          //        .apply(fakePutRequest).futureValue
          //
          //      info("Get the business registration retails with the same userId and formTypeRef")
          //      val response = submissionExtraDataController.getBusinessRegistrationDetails(testUserId, testFormTypeRef)
          //        .apply(fakeGetRequest).futureValue
          //
          //      Then("response should be OK")
          //      response.header.status shouldBe Status.OK
          //
          //      Then("response body should be some business registration details")
          //      Json.parse(consume(response.body)).toString() shouldBe s"""${Json.parse(fakeBusinessDetails)}"""
          //
          //      Then("update a form id using the same userId and formTypeRef")
          //      val updateFormIdResponse = submissionExtraDataController.updateFormId(testUserId, testFormTypeRef)
          //        .apply(fakePutRequestForUpdateFormId).futureValue
          //      updateFormIdResponse.header.status shouldBe Status.ACCEPTED
          //
          //      info("Get the business registration retails with different userId and formTypeRef")
          //      val notFoundResponse = submissionExtraDataController.getBusinessRegistrationDetails("11", "22")
          //        .apply(fakeGetRequest).futureValue
          //
          //      Then("response is not found")
          //      notFoundResponse.header.status shouldBe Status.NOT_FOUND
          //
          //    }
          //  }
          =======
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
  >>>>>>>
  5 bf8803ddd7df45a395d590c647b433917121f4d

}
