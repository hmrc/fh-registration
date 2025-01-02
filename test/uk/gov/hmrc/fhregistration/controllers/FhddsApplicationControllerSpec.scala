/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.fhregistration.controllers


import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.fhregistration.actions.{Actions, UserAction, UserGroupAction}
import uk.gov.hmrc.fhregistration.connectors.{DesConnector, EmailConnector, TaxEnrolmentConnector}
import uk.gov.hmrc.fhregistration.models.des._
import uk.gov.hmrc.fhregistration.models.fhdds._
import uk.gov.hmrc.fhregistration.repositories.{DefaultSubmissionTrackingRepository, SubmissionTracking}
import uk.gov.hmrc.fhregistration.services.{AuditService, SubmissionTrackingService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FhddsApplicationControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures with Results {

  private val mockDesConnector = mock[DesConnector]
  private val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  private val mockEmailConnector = mock[EmailConnector]
  private val mockSubmissionTrackingService = mock[SubmissionTrackingService]
  private val mockAuditService = mock[AuditService]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockRepo = mock[DefaultSubmissionTrackingRepository]
  private val mockActions = mock[Actions]
  private val cc: ControllerComponents = Helpers.stubControllerComponents()
  private val mockAuthConnector = mock[AuthConnector]


  val application: Application = new GuiceApplicationBuilder()
    .build()
  val mcc: MessagesControllerComponents = application.injector.instanceOf[MessagesControllerComponents]




  implicit val materializer: Materializer = mock[Materializer]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val controller = new FhddsApplicationController(
    mockDesConnector,
    mockTaxEnrolmentConnector,
    mockEmailConnector,
    mockSubmissionTrackingService,
    mockAuditService,
    mockAuditConnector,
    cc,
    mockActions,
    mockRepo
  )

  private def dateFromString(dateString: String): Date =
    new SimpleDateFormat("yyyy-MM-dd").parse(dateString)

  "FhddsApplicationController" should {

    "return all submissions on findAllSubmissions" in {
      when(mockRepo.findAll()).thenReturn(Future.successful(List.empty))

      val result: Future[Result] = controller.findAllSubmissions()(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(List.empty[JsObject])
    }

    "return a submission by formBundleId on getSubmission" in {
      val formBundleId = "12345"
      val mockSubmissionTracking = SubmissionTracking(
        userId = "user123",
        formBundleId = formBundleId,
        email = "user123@example.com",
        submissionTime = System.currentTimeMillis(),
        enrolmentProgressOpt = Some(EnrolmentProgress.Pending),
        registrationNumber = Some("reg123")
      )
      when(mockRepo.findSubmissionTrackingByFormBundleId(formBundleId))
        .thenReturn(Future.successful(Some(mockSubmissionTracking)))
      val result: Future[Result] = controller.getSubmission(formBundleId)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(mockSubmissionTracking)
    }










    "handle amendments in amend" in {
      val fhddsRegistrationNumber = "reg123"
      val submissionRequest = SubmissionRequest(
        emailAddress = "email@example.com",
        submission = Json.obj("key" -> "value")
      )
      val request = FakeRequest(PUT, routes.FhddsApplicationController.amend(fhddsRegistrationNumber).url)
        .withBody(submissionRequest)
        .withHeaders(CONTENT_TYPE -> JSON)

      when(mockActions.userAction) thenReturn new UserAction(mockAuthConnector, cc)

      when(mockAuthConnector.authorise(any(), any[Retrieval[Any]]())(any(), any()))
        .thenReturn(Future.failed(new NoActiveSession("No active session") {}))

      val desSubmissionResponse = DesSubmissionResponse(
        registrationNumberFHDDS = "reg123",
        processingDate = dateFromString("2023-12-01"),
        etmpFormBundleNumber = "formBundle123"
      )
      when(mockDesConnector.sendAmendment(any(), any())(any()))
        .thenReturn(Future.successful(desSubmissionResponse))

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      when(mockEmailConnector.sendEmail(any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful((): Unit))

      val result = controller.amend(fhddsRegistrationNumber)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(
        SubmissionResponse(desSubmissionResponse.registrationNumberFHDDS, desSubmissionResponse.processingDate)
      )
    }



    "handle withdrawals in withdrawal" in {
      val fhddsRegistrationNumber = "XMFH00000123456"
      val userId = "user123"
      val groupId = "group456"
      val registrationNumber = "testRegNumber"
      val withdrawalRequest = WithdrawalRequest("email@example.com", Json.obj())
      val desWithdrawalResponse = DesWithdrawalResponse(dateFromString("2023-12-01"))
      val request = FakeRequest().withBody(Json.toJson(withdrawalRequest)).withHeaders(
        "X-User-Id" -> userId,
        "X-Group-Id" -> groupId
      )
      val enrolments = Enrolments(
        Set(
          Enrolment(
            key = "HMRC-OBTDS-ORG",
            identifiers = Seq(EnrolmentIdentifier("ETMPREGISTRATIONNUMBER", fhddsRegistrationNumber)),
            state = "Activated",
            delegatedAuthRule = None
          )
        )
      )


      when(mockActions.userGroupAction) thenReturn new UserGroupAction(mockAuthConnector, cc)

      when(
        mockAuthConnector.authorise(
          any(),
          any[Retrieval[~[Option[String], Enrolments]]]())(any(), any())
      ) thenReturn Future.successful(new ~[Option[String], Enrolments](Some(userId), enrolments))


      when(mockDesConnector.sendWithdrawal(any(), any())(any()))
        .thenReturn(Future.successful(desWithdrawalResponse))

//      when(mockAuditService.buildSubmissionWithdrawalAuditEvent(any(), any()))
//        .thenReturn(mock[DataEvent])

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(Success))

      val result = controller.withdrawal(fhddsRegistrationNumber)(request)
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(desWithdrawalResponse.processingDate)
    }
















    "handle deregistration in deregister" in {
      val fhddsRegistrationNumber = "XMFH00000123456"
      val userId = "user123"
      val groupId = "group456"

      val deregistrationRequest = DeregistrationRequest(
        "email@example.com",
        Json.obj("deregistrationReason" -> "NoLongerNeeded")
      )

      val request = FakeRequest()
        .withHeaders(
          "X-User-Id" -> userId,
          "X-Group-Id" -> groupId
        )
        .withBody(deregistrationRequest)

      val desDeregistrationResponse = DesDeregistrationResponse(dateFromString("2023-12-01"))

      when(mockActions.userGroupAction).thenReturn(new UserGroupAction(mockAuthConnector, mcc))
      when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, mcc))

      when(mockAuthConnector.authorise(any(), any[Retrieval[((((Enrolments, Option[String]), Option[String]), Option[AffinityGroup]), Option[CredentialRole])]]())(any(), any()))
        .thenReturn(
          Future.successful((((
            Enrolments(Set(Enrolment("HMRC-OBTDS-ORG", Seq(EnrolmentIdentifier("ETMPREGISTRATIONNUMBER", fhddsRegistrationNumber)), "Activated"))),
            Some(userId)),
            Some(groupId)),
            Some(AffinityGroup.Organisation)),
            Some(User)
          ))

      when(mockDesConnector.sendDeregistration(any(), any())(any()))
        .thenReturn(Future.successful(desDeregistrationResponse))

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      val result = controller.deregister(fhddsRegistrationNumber)(request)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(desDeregistrationResponse.processingDate)
    }













    "return subscription status in checkStatus" in {
      val fhddsRegistrationNumber = "reg123"
      val statusResponse = StatusResponse(DesStatus.Successful, Some("idTypeValue"), Some("idValueValue"))

      when(mockDesConnector.getStatus(any())(any()))
        .thenReturn(Future.successful(statusResponse))

      val result = controller.checkStatus(fhddsRegistrationNumber)(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(FhddsStatus.Approved)
    }

    "return enrolment progress" in {
      val userId = "user123"
      val request = FakeRequest().withHeaders("X-User-Id" -> userId)

      val enrolments = Enrolments(Set.empty)

      when(mockActions.userAction) thenReturn new UserAction(mockAuthConnector, cc)

      when(
        mockAuthConnector.authorise(any(), any[Retrieval[~[Option[String], Enrolments]]]())(any(), any())
      ) thenReturn Future.successful(new ~[Option[String], Enrolments](Some(userId), enrolments))

      when(mockSubmissionTrackingService.enrolmentProgress(any(), any()))
        .thenReturn(Future.successful(EnrolmentProgress.Pending))

      val result = controller.enrolmentProgress(request)
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(EnrolmentProgress.Pending)
    }


    "return BadRequest for amend when user id is not found" in {
      val fhddsRegistrationNumber = "reg123"
      val submissionRequest = SubmissionRequest(
        emailAddress = "email@example.com",
        submission = Json.obj("key" -> "value")
      )
      val request = FakeRequest()
        .withBody(Json.toJson(submissionRequest))
        .withHeaders(CONTENT_TYPE -> JSON)

      when(mockActions.userAction) thenReturn new UserAction(mockAuthConnector, cc)

      when(mockAuthConnector.authorise(any(), any[Retrieval[~[Option[String], Enrolments]]]())(any(), any()))
        .thenReturn(Future.successful(new ~[Option[String], Enrolments](None, Enrolments(Set.empty))))

      val result = controller.amend(fhddsRegistrationNumber)(request)

      status(result) mustBe BAD_REQUEST
    }
  }
}
