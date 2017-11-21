package uk.gov.hmrc.fhdds

import org.mockito.ArgumentMatchers.{any, eq ⇒ matcherEq}
import org.mockito.Mockito
import play.mvc.Http.Status
import uk.gov.hmrc.fhdds.Services.FakeData._
import uk.gov.hmrc.fhdds.Services.FhddsApplicationIntegrationMocks
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class FhddsApplicationFhddsApplicationIntegrationMocks extends FhddsApplicationIntegrationMocks {

  implicit val ec = ExecutionContext.global

  feature("Get safeId") {

    scenario("When the service try to get a safeId of an application with a valid submissionRef and services are working well") {

      info("Get data from dfs store")
      Mockito
        .when(mockDfsStoreConnector.getSubmission(validFakeSubmissionRef))
        .thenReturn(Future successful aFakeSubmission)

      info("Get the extra data from fhdds database")
      Mockito
        .when(mockSubmissionExtraDataRepository.findSubmissionExtraData(aFakeSubmission.formId))
        .thenReturn(Future successful Option(aFakeSubmissionExtraData))

      Then("Get the safe id")
      val response = fhddsApplicationController
        .getSafeId(validFakeSubmissionRef)
        .apply(fakeGetRequest).futureValue

      info("response should be OK")
      response.header.status shouldBe Status.OK

      val safeId = aFakeSubmissionExtraData.businessRegistrationDetails.safeId
      info(s"safe id should be $safeId")
      consume(response.body) shouldBe s""""$safeId""""
    }
  }


  feature("Get an application") {

    scenario("When the service try to get an application details with a valid submissionRef and services are working well") {

      info("Get data from dfs store")
      Mockito
        .when(mockDfsStoreConnector.getSubmission(validFakeSubmissionRef))
        .thenReturn(Future successful aFakeSubmission)

      info("Get the extra data from fhdds database")
      Mockito
        .when(mockSubmissionExtraDataRepository.findSubmissionExtraData(aFakeSubmission.formId))
        .thenReturn(Future successful Option(aFakeSubmissionExtraData))

      Then("Get the application data")
      val response = fhddsApplicationController
        .getApplication(validFakeSubmissionRef)
        .apply(fakeGetRequest).futureValue

      info("response should be OK")
      response.header.status shouldBe Status.OK
    }

    scenario("When the service try to get an application details, but details can not be found in dfs store") {

      info("Try to get data from dfs store, but got nothing")
      Mockito
        .when(mockDfsStoreConnector.getSubmission(validFakeSubmissionRef))
        .thenReturn(Future successful aFakeInvalidSubmission)

      Then("Try to get the application data")
      val response = fhddsApplicationController
        .getApplication(validFakeSubmissionRef)
        .apply(fakeGetRequest)

      info("should coursing exception")
      an [Exception] should be thrownBy Await.result(response, Duration.Inf)
    }

    scenario("When the service try to get an application details, but fhdds can not get the extra data") {

      info("Get data from dfs store")
      Mockito
        .when(mockDfsStoreConnector.getSubmission(validFakeSubmissionRef))
        .thenReturn(Future successful aFakeSubmission)

      Then("Can not get the extra data from fhdds database")

      Then("Try to get the application data")
      val response = fhddsApplicationController
        .getApplication(validFakeSubmissionRef)
        .apply(fakeGetRequest)

      info("should coursing exception")
      an [Exception] should be thrownBy Await.result(response, Duration.Inf)
    }
  }


  feature("Submit an application") {

    scenario("When the service try to submit an application") {

      info("Get the extra data from fhdds database")
      Mockito
        .when(mockSubmissionExtraDataRepository.findSubmissionExtraData(formId = fakePostRequest.body.formId))
        .thenReturn(Future successful Option(aFakeSubmissionExtraData))

      Then("Creates an application for DES")
      val application = createDesSubmission(validFormData, aFakeSubmissionExtraData)

      Then("submit the application to DES, and get des response")
      Mockito
        .when(mockDesConnector.sendSubmission(matcherEq(aFakeSubmissionExtraData.businessRegistrationDetails.safeId),
          matcherEq(application))(any[HeaderCarrier]()))
        .thenReturn(Future successful aFakeDesSubmissionResponse)

      Then("submit to tax enrolment, and get tax enrolment response")
      Mockito
        .when(mockTaxEnrolmentConnector.subscribe(subscriptionId = matcherEq(aFakeDesSubmissionResponse.registrationNumberFHDDS),
                                                  safeId = matcherEq(aFakeSubmissionExtraData.businessRegistrationDetails.safeId),
                                                  authorization = matcherEq(None))(any[HeaderCarrier]()))
        .thenReturn(Future successful Some(aFakeJsonObject))

      val response = fhddsApplicationController.submit()(fakePostRequest).futureValue

      Then("response for the submit should be OK")
      response.header.status shouldBe Status.OK
    }
  }

}
