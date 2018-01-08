package uk.gov.hmrc.fhregistration

import org.mockito.ArgumentMatchers.{any, eq ⇒ matcherEq}
import org.mockito.Mockito
import play.api.libs.json.{JsObject, JsString, JsValue}
import play.mvc.Http.Status
import uk.gov.hmrc.fhregistration.services.FakeData._
import uk.gov.hmrc.fhregistration.services.FhddsApplicationIntegrationMocks
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.duration._
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
        .when(mockSubmissionExtraDataRepository.updateRegistrationNumber(any[String](), any[String](), any[String]()))
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
                                                  safeId = matcherEq(aFakeSubmissionExtraData.businessRegistrationDetails.safeId),
                                                  authorization = matcherEq(None))(any[HeaderCarrier]()))
        .thenReturn(Future successful Some(aFakeJsonObject))

      val response = fhddsApplicationController.submit()(fakePostRequest).futureValue

      Then("response for the submit should be OK")
      response.header.status shouldBe Status.OK
    }
  }

  feature("retrieve an mdtp subscription status from DES") {

    scenario("When bta calls fh-registration to retrieve a status and gets a 200 response") {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))
      val fakeDesResponse: HttpResponse = HttpResponse(200, Some(json))

      Mockito
        .when(
          mockDesConnector.getStatus(matcherEq("registrationID"))(any[HeaderCarrier]())
        )
        .thenReturn(Future successful fakeDesResponse)

      val response = fhddsApplicationController.checkStatus("registrationID")(fakeGetRequest)

      Then("response for the submit should be OK")
      Await.result(response, 500.millis).header.status shouldBe Status.OK
      consume(Await.result(response, 500.millis).body) shouldBe "Received"
    }
  }

  {

    scenario("When bta calls fh-registration to retrieve a status and receives a 400") {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))
      val fakeDesResponse: HttpResponse = HttpResponse(400, Some(json))

      Mockito
        .when(
          mockDesConnector.getStatus(matcherEq("registrationID"))(any[HeaderCarrier]())
        )
        .thenReturn(Future successful fakeDesResponse)

      val response = fhddsApplicationController.checkStatus("registrationID")(fakeGetRequest)

      Then("response for the submit should be BadRequest")
      Await.result(response, 500.millis).header.status shouldBe Status.BAD_REQUEST
      val ext = "Submission has not passed validation. Invalid parameter FHDDS Registration Number."
      consume(Await.result(response, 500.millis).body) shouldBe ext
    }

    scenario("When bta calls fh-registration to retrieve a status and receives a 404") {

      val json: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))
      val fakeDesResponse: HttpResponse = HttpResponse(404, Some(json))

      Mockito
        .when(
          mockDesConnector.getStatus(matcherEq("registrationID"))(any[HeaderCarrier]())
        )
        .thenReturn(Future successful fakeDesResponse)

      val response = fhddsApplicationController.checkStatus("registrationID")(fakeGetRequest)

      Then("response for the submit should be NotFound")
      Await.result(response, 500.millis).header.status shouldBe Status.NOT_FOUND
      val ext = "No SAP Number found for the provided FHDDS Registration Number."
      consume(Await.result(response, 500.millis).body) shouldBe ext
    }

    scenario("When bta calls fh-registration to retrieve a status and receives a 403") {

      val fakeDesResponse: HttpResponse = HttpResponse(403, None)

      Mockito
        .when(
          mockDesConnector.getStatus(matcherEq("registrationID"))(any[HeaderCarrier]())
        )
        .thenReturn(Future successful fakeDesResponse)

      val response = fhddsApplicationController.checkStatus("registrationID")(fakeGetRequest)

      Then("response for the submit should be Forbidden")
      Await.result(response, 500.millis).header.status shouldBe Status.FORBIDDEN
      val ext = "Unexpected business error received."
      consume(Await.result(response, 500.millis).body) shouldBe ext
    }


    scenario("When bta calls fh-registration to retrieve a status and receives a 500") {

      val fakeDesResponse: HttpResponse = HttpResponse(500, None)

      Mockito
        .when(
          mockDesConnector.getStatus(matcherEq("registrationID"))(any[HeaderCarrier]())
        )
        .thenReturn(Future successful fakeDesResponse)

      val response = fhddsApplicationController.checkStatus("registrationID")(fakeGetRequest)

      Then("response for the submit should be Internal server error")
      Await.result(response, 500.millis).header.status shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }

}
