package uk.gov.hmrc.fhregistration

import org.mockito.ArgumentMatchers.{any, eq ⇒ matcherEq}
import org.mockito.Mockito
import play.mvc.Http.Status
import uk.gov.hmrc.fhregistration.services.FakeData._
import uk.gov.hmrc.fhregistration.services.FhddsApplicationIntegrationMocks
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class FhddsApplicationIntegrationSpecs extends FhddsApplicationIntegrationMocks {

  implicit val ec = ExecutionContext.global

  feature("Submit an application") {

    scenario("When the service try to submit an application") {

      info("Get the extra data from fhdds database")
      Mockito
        .when(mockSubmissionExtraDataRepository.findSubmissionExtraDataByFormId(formId = fakePostRequest.body.formId))
        .thenReturn(Future successful Option(aFakeSubmissionExtraData))

      Mockito
        .when(mockSubmissionExtraDataRepository
          .updateRegistrationNumberWithETMPFormBundleNumber(any[String](), any[String](), any[String](), any[String]()))
        .thenReturn(Future successful true)

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
                                                  etmpFormBundleNumber = matcherEq(testETMPFormBundleNumber),
                                                  authorization = matcherEq(None))(any[HeaderCarrier]()))
        .thenReturn(Future successful Some(aFakeJsonObject))

      val response = fhddsApplicationController.submit()(fakePostRequest).futureValue

      Then("response for the submit should be OK")
      response.header.status shouldBe Status.OK
    }
  }

}
