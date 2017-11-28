package uk.gov.hmrc.fhdds

import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.fhdds.Services.FakeData._
import uk.gov.hmrc.fhdds.Services.SubMissionExtraDataIntegrationMocks

import scala.concurrent.ExecutionContext


class SubmissionExtraDataMocksFhddsApplication extends SubMissionExtraDataIntegrationMocks {

  implicit val ec = ExecutionContext.global

  feature("Get Business Registration Details") {

    scenario("With valid or not valid userId and formTypeRef") {

      info("Save a business registration retails with userId and formTypeRef")
      submissionExtraDataController.saveBusinessRegistrationDetails(testUserId, testFormTypeRef)
        .apply(fakePutRequest).futureValue

      info("Get the business registration retails with the same userId and formTypeRef")
      val response = submissionExtraDataController.getBusinessRegistrationDetails(testUserId, testFormTypeRef)
                       .apply(fakeGetRequest).futureValue

      Then("response should be OK")
      response.header.status shouldBe Status.OK

      Then("response body should be some business registration details")
      Json.parse(consume(response.body)).toString() shouldBe s"""${Json.parse(fakeBusinessDetails)}"""

      Then("update a form id using the same userId and formTypeRef")
      val updateFormIdResponse = submissionExtraDataController.updateFormId(testUserId, testFormTypeRef)
                                   .apply(fakePutRequestForUpdateFormId).futureValue
      updateFormIdResponse.header.status shouldBe Status.ACCEPTED

      info("Get the business registration retails with different userId and formTypeRef")
      val notFoundResponse = submissionExtraDataController.getBusinessRegistrationDetails("11", "22")
                               .apply(fakeGetRequest).futureValue

      Then("response is not found")
      notFoundResponse.header.status shouldBe Status.NOT_FOUND

    }
  }

}
