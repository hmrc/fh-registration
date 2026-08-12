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

import cats.data.OptionT
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.fhregistration.actions.{Actions, UserAction, UserGroupAction}
import uk.gov.hmrc.fhregistration.connectors.{DesConnector, DesSubmissionException, EmailConnector, HipConnector, HipSubmissionException, TaxEnrolmentConnector}
import uk.gov.hmrc.fhregistration.models.TaxEnrolmentsCallback
import uk.gov.hmrc.fhregistration.models.des.*
import uk.gov.hmrc.fhregistration.models.fhdds.*
import uk.gov.hmrc.fhregistration.models.hip.*
import uk.gov.hmrc.fhregistration.repositories.{DefaultSubmissionTrackingRepository, SubmissionTracking}
import uk.gov.hmrc.fhregistration.services.{AuditService, SubmissionTrackingService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FhddsApplicationControllerSpec
    extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfterEach with Results {

  private val mockDesConnector = mock[DesConnector]
  private val mockHipConnector: HipConnector = mock[HipConnector]
  private val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  private val mockTaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  private val mockEmailConnector = mock[EmailConnector]
  private val mockSubmissionTrackingService = mock[SubmissionTrackingService]
  private val mockAuditService = mock[AuditService]
  private val mockAuditConnector = mock[AuditConnector]
  private val mockRepo = mock[DefaultSubmissionTrackingRepository]
  private val mockActions = mock[Actions]
  private val mcc: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()
  private val cc: ControllerComponents = mcc
  private val mockAuthConnector = mock[AuthConnector]

  implicit val materializer: Materializer = mock[Materializer]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit =
    reset(mockServicesConfig)

  private val controller =
    new FhddsApplicationController(
      mockDesConnector,
      mockHipConnector,
      mockServicesConfig,
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

    Seq(("HIP", true), ("DES", false)).foreach { case (etmp, hipFlag) =>
      s"handle amendments in $etmp amend" in {
        val fhddsRegistrationNumber = "reg123"
        val submissionRequest = SubmissionRequest(
          emailAddress = "email@example.com",
          submission = Json.obj("key" -> "value")
        )
        val request = FakeRequest(PUT, routes.FhddsApplicationController.amend(fhddsRegistrationNumber).url)
          .withBody(submissionRequest)
          .withHeaders(CONTENT_TYPE -> JSON)

        when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, cc))

        when(mockAuthConnector.authorise(any(), any[Retrieval[Any]]())(using any(), any()))
          .thenReturn(Future.failed(new NoActiveSession("No active session") {}))

        val hipSubmissionResponse = HipSubmissionResponse(
          registrationNumberFHDDS = "reg123",
          processingDate = dateFromString("2023-12-01"),
          etmpFormBundleNumber = Some("formBundle123")
        )

        if (hipFlag)
          when(mockHipConnector.createOrUpdateFhdds(any(), any(), any())(any()))
            .thenReturn(Future.successful(hipSubmissionResponse))
        else
          when(mockDesConnector.sendAmendment(any(), any())(any()))
            .thenReturn(Future.successful(hipSubmissionResponse.toDesSubmissionResponse))

        when(mockAuditConnector.sendEvent(any())(using any(), any())).thenReturn(Future.successful(AuditResult.Success))

        when(mockEmailConnector.sendEmail(any(), any(), any())(using any(), any(), any()))
          .thenReturn(Future.successful((): Unit))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.amend(fhddsRegistrationNumber)(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(
          SubmissionResponse(hipSubmissionResponse.registrationNumberFHDDS, hipSubmissionResponse.processingDate)
        )
      }

      s"return the $etmp business error body for subscribe when submission fails" in {
        val submissionRequest = SubmissionRequest(
          emailAddress = "email@example.com",
          submission = Json.obj("key" -> "value")
        )
        val request = FakeRequest(POST, routes.FhddsApplicationController.subscribe("safe123", None).url)
          .withBody(submissionRequest)
          .withHeaders(CONTENT_TYPE -> JSON)

        val mockInternalId = "mockUserId"
        val mockGroupId = "mockGroupId"
        val mockRetrieval: Option[String] ~ Option[String] = new ~(Some(mockInternalId), Some(mockGroupId))

        when(mockActions.userGroupAction).thenReturn(new UserGroupAction(mockAuthConnector, mcc))
        when(
          mockAuthConnector.authorise(
            any(),
            any[Retrieval[Option[String] ~ Option[String]]]()
          )(using any[HeaderCarrier](), any())
        ).thenReturn(Future.successful(mockRetrieval))

        if (hipFlag)
          when(mockHipConnector.createOrUpdateFhdds(any(), any(), any())(any()))
            .thenReturn(Future.failed(HipSubmissionException(403, "ACTIVE_SUBSCRIPTION", "already active")))
        else
          when(mockDesConnector.sendSubmission(any(), any())(any()))
            .thenReturn(Future.failed(DesSubmissionException(403, "ACTIVE_SUBSCRIPTION", "already active")))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.subscribe("safe123", None)(request)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj("code" -> "ACTIVE_SUBSCRIPTION", "reason" -> "already active")
      }

      s"return the $etmp business error body for amend when submission fails" in {
        val fhddsRegistrationNumber = "reg123"
        val submissionRequest = SubmissionRequest(
          emailAddress = "email@example.com",
          submission = Json.obj("key" -> "value")
        )
        val request = FakeRequest(PUT, routes.FhddsApplicationController.amend(fhddsRegistrationNumber).url)
          .withBody(submissionRequest)
          .withHeaders(CONTENT_TYPE -> JSON)

        when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, cc))
        when(mockAuthConnector.authorise(any(), any[Retrieval[Any]]())(using any(), any()))
          .thenReturn(Future.failed(new NoActiveSession("No active session") {}))

        if (hipFlag)
          when(mockHipConnector.createOrUpdateFhdds(any(), any(), any())(any()))
            .thenReturn(Future.failed(HipSubmissionException(403, "ACTIVE_SUBSCRIPTION", "already active")))
        else
          when(mockDesConnector.sendAmendment(any(), any())(any()))
            .thenReturn(Future.failed(DesSubmissionException(403, "ACTIVE_SUBSCRIPTION", "already active")))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.amend(fhddsRegistrationNumber)(request)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj("code" -> "ACTIVE_SUBSCRIPTION", "reason" -> "already active")
      }

      s"handle withdrawals in $etmp withdrawal" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val userId = "user123"
        val groupId = "group456"

        val withdrawalRequest = WithdrawalRequest(
          emailAddress = "email@example.com",
          withdrawal = Json.obj("reason" -> "reason for withdrawal")
        )

        val hipWithdrawalResponse = HipWithdrawalResponse(
          processingDate = dateFromString("2023-12-01")
        )

        val request = FakeRequest()
          .withHeaders(
            "X-User-Id"  -> userId,
            "X-Group-Id" -> groupId
          )
          .withBody(withdrawalRequest)

        val mockInternalId = "mockUserId"
        val mockGroupId = "mockGroupId"
        val mockRetrieval: Option[String] ~ Option[String] = new ~(Some(mockInternalId), Some(mockGroupId))

        when(mockActions.userGroupAction).thenReturn(new UserGroupAction(mockAuthConnector, mcc))
        when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, mcc))

        when(
          mockAuthConnector.authorise(
            any(),
            any[Retrieval[Option[String] ~ Option[String]]]()
          )(using any[HeaderCarrier](), any())
        )
          .thenReturn(Future.successful(mockRetrieval))

        when(mockEmailConnector.sendEmail(any(), any(), any())(using any(), any(), any()))
          .thenReturn(Future.successful((): Unit))

        if (hipFlag)
          when(mockHipConnector.subscriptionWithdrawal(any(), any())(any()))
            .thenReturn(Future.successful(hipWithdrawalResponse))
        else
          when(mockDesConnector.sendWithdrawal(any(), any())(any()))
            .thenReturn(Future.successful(hipWithdrawalResponse.toDesWithdrawalResponse))

        when(mockAuditConnector.sendEvent(any())(using any(), any())).thenReturn(Future.successful(AuditResult.Success))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.withdrawal(fhddsRegistrationNumber)(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(hipWithdrawalResponse.processingDate)
      }

      s"handle deregistration in $etmp deregister" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val userId = "user123"
        val groupId = "group456"

        val deregistrationRequest = DeregistrationRequest(
          "email@example.com",
          Json.obj("deregistrationReason" -> "NoLongerNeeded")
        )

        val request = FakeRequest()
          .withHeaders(
            "X-User-Id"  -> userId,
            "X-Group-Id" -> groupId
          )
          .withBody(deregistrationRequest)

        val hipDeregistrationResponse = HipDeregistrationResponse(dateFromString("2023-12-01"))

        val mockInternalId = "mockUserId"
        val mockGroupId = "mockGroupId"
        val mockRetrieval: Option[String] ~ Option[String] = new ~(Some(mockInternalId), Some(mockGroupId))

        when(mockActions.userGroupAction).thenReturn(new UserGroupAction(mockAuthConnector, mcc))
        when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, mcc))

        when(
          mockAuthConnector.authorise(
            any(),
            any[Retrieval[Option[String] ~ Option[String]]]()
          )(using any[HeaderCarrier](), any())
        )
          .thenReturn(Future.successful(mockRetrieval))

        when(mockEmailConnector.sendEmail(any(), any(), any())(using any(), any(), any()))
          .thenReturn(Future.successful((): Unit))

        if (hipFlag)
          when(mockHipConnector.subscriptionDeregistration(any(), any())(any()))
            .thenReturn(Future.successful(hipDeregistrationResponse))
        else
          when(mockDesConnector.sendDeregistration(any(), any())(any()))
            .thenReturn(Future.successful(hipDeregistrationResponse.toDesDeregistrationResponse))

        when(mockAuditConnector.sendEvent(any())(using any(), any())).thenReturn(Future.successful(AuditResult.Success))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.deregister(fhddsRegistrationNumber)(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(hipDeregistrationResponse.processingDate)
      }

      s"return subscription status in $etmp checkStatus" in {
        val fhddsRegistrationNumber = "reg123"
        val hipStatusResponse =
          SubscriptionStatusResponse(HipStatus.Successful, Some("idTypeValue"), Some("idValueValue"))

        if (hipFlag)
          when(mockHipConnector.getStatus(any())(any())).thenReturn(Future.successful(hipStatusResponse))
        else
          when(mockDesConnector.getStatus(any())(any()))
            .thenReturn(Future.successful(hipStatusResponse.toDesStatusResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.checkStatus(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(FhddsStatus.Approved)
      }

      s"return $etmp subscription data for a valid FHDDS registration number" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.status).thenReturn(200)
        when(httpResponse.json).thenReturn(Json.obj("key" -> "value"))

        if (hipFlag)
          when(mockHipConnector.subscriptionDisplay(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))
        else
          when(mockDesConnector.display(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.get(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("key" -> "value")
      }

      s"handle bad request from $etmp connector" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.status).thenReturn(400)

        if (hipFlag)
          when(mockHipConnector.subscriptionDisplay(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))
        else
          when(mockDesConnector.display(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.get(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Submission has not passed validation")
      }

      s"handle not found from $etmp connector" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.status).thenReturn(404)

        if (hipFlag)
          when(mockHipConnector.subscriptionDisplay(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))
        else
          when(mockDesConnector.display(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.get(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include("No SAP Number found")
      }

      s"handle 422 Validation errors from $etmp connector" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.status).thenReturn(422)
        when(httpResponse.body).thenReturn(
          """{"errors":{"code":"002","processingDate":"2026-03-09T12:34:46Z","text":"ID not found"}}"""
        )

        if (hipFlag)
          when(mockHipConnector.subscriptionDisplay(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))
        else
          when(mockDesConnector.display(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.get(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe NOT_FOUND
        contentAsString(result) must include("Validation errors")
        contentAsString(result) must include("ID not found")
      }

      s"handle forbidden response from $etmp connector" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.status).thenReturn(403)

        if (hipFlag)
          when(mockHipConnector.subscriptionDisplay(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))
        else
          when(mockDesConnector.display(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.get(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe FORBIDDEN
        contentAsString(result) must include("Unexpected business error received")
      }

      s"handle unexpected error from $etmp connector" in {
        val fhddsRegistrationNumber = "XMFH00000123456"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.status).thenReturn(500)
        when(httpResponse.body).thenReturn("Internal server error")

        if (hipFlag)
          when(mockHipConnector.subscriptionDisplay(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))
        else
          when(mockDesConnector.display(eqTo(fhddsRegistrationNumber))(any()))
            .thenReturn(Future.successful(httpResponse))

        when(mockServicesConfig.getBoolean("features.hip")).thenReturn(hipFlag)
        val result = controller.get(fhddsRegistrationNumber)(FakeRequest())

        status(result) mustBe BAD_GATEWAY
        contentAsString(result) must include("ETMP API is currently experiencing problems")
      }
    }

    "return BadGateway when HIP creation response has no etmpFormBundleNumber" in {
      val submissionRequest = SubmissionRequest(
        emailAddress = "test@email.com",
        submission = Json.obj("someKey" -> "someValue")
      )
      val request = FakeRequest(POST, routes.FhddsApplicationController.subscribe("safe123", None).url)
        .withBody(submissionRequest)
        .withHeaders(CONTENT_TYPE -> JSON)

      when(mockActions.userGroupAction).thenReturn(new UserGroupAction(mockAuthConnector, mcc))
      when(
        mockAuthConnector.authorise(
          any(),
          any[Retrieval[Option[String] ~ Option[String]]]()
        )(using any[HeaderCarrier](), any())
      ).thenReturn(Future.successful(new ~(Some("someUserId"), Some("someGroupId"))))

      val hipSubmissionResponse = HipSubmissionResponse(
        registrationNumberFHDDS = "reg123",
        processingDate = dateFromString("2023-12-01"),
        etmpFormBundleNumber = None
      )

      when(mockHipConnector.createOrUpdateFhdds(any(), any(), any())(any()))
        .thenReturn(Future.successful(hipSubmissionResponse))

      when(mockServicesConfig.getBoolean("features.hip")).thenReturn(true)
      val result = controller.subscribe("safe123", None)(request)

      status(result) mustBe BAD_GATEWAY
      contentAsJson(result) mustBe Json.obj(
        "code"   -> "EtmpFormBundleNumber is missing in HIP response",
        "reason" -> "HIP creation response for registration reg123 did not include an etmpFormBundleNumber"
      )
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

      when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, cc))

      when(mockAuthConnector.authorise(any(), any[Retrieval[~[Option[String], Enrolments]]]())(using any(), any()))
        .thenReturn(Future.successful(new ~[Option[String], Enrolments](None, Enrolments(Set.empty))))

      val result = controller.amend(fhddsRegistrationNumber)(request)

      status(result) mustBe BAD_REQUEST
    }

    "return enrolment progress" in {
      val userId = "user123"
      val request = FakeRequest().withHeaders("X-User-Id" -> userId)

      val enrolments = Enrolments(Set.empty)

      when(mockActions.userAction).thenReturn(new UserAction(mockAuthConnector, cc))

      when(
        mockAuthConnector.authorise(any(), any[Retrieval[~[Option[String], Enrolments]]]())(using any(), any())
      ).thenReturn(Future.successful(new ~[Option[String], Enrolments](Some(userId), enrolments)))

      when(mockSubmissionTrackingService.enrolmentProgress(any(), any()))
        .thenReturn(Future.successful(EnrolmentProgress.Pending))

      val result = controller.enrolmentProgress(request)
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(EnrolmentProgress.Pending)
    }

    "handle subscription callback with SUCCEEDED state" in {
      val formBundleId = "formBundle123"
      val callback = TaxEnrolmentsCallback(
        url = "http://test/callback",
        state = "SUCCEEDED",
        errorResponse = None
      )
      val request =
        FakeRequest().withJsonBody(Json.toJson(callback))

      when(mockSubmissionTrackingService.getSubmissionTrackingEmail(formBundleId))
        .thenReturn(OptionT(Future.successful(Some("email@example.com"): Option[String])))

      when(mockSubmissionTrackingService.deleteSubmissionTracking(formBundleId))
        .thenAnswer(_ => Future.successful(()))

      when(mockEmailConnector.sendEmail(any(), any(), any())(using any(), any(), any()))
        .thenReturn(Future.successful((): Unit))

      val result = controller.subscriptionCallback(formBundleId)(request)

      verify(mockEmailConnector, times(2)).sendEmail(
        eqTo(mockEmailConnector.defaultEmailTemplateID),
        eqTo(UserData("email@example.com")),
        eqTo(Map.empty[String, String])
      )(using any(), any(), any())
    }

    "handle subscription callback with FAILED state" in {
      val formBundleId = "formBundle123"
      val callback = TaxEnrolmentsCallback(
        url = "http://test/callback",
        state = "FAILED",
        errorResponse = Some("Error message")
      )
      val request = FakeRequest().withBody(Json.toJson(callback))

      val result = controller.subscriptionCallback(formBundleId)(request)

      verify(mockSubmissionTrackingService, never()).deleteSubmissionTracking(any())
    }

    "handle missing submission tracking data during callback" in {
      val formBundleId = "formBundle123"
      val callback = TaxEnrolmentsCallback(
        url = "http://test/callback",
        state = "SUCCEEDED",
        errorResponse = None
      )
      val request = FakeRequest().withBody(Json.toJson(callback))

      when(mockSubmissionTrackingService.getSubmissionTrackingEmail(formBundleId))
        .thenReturn(OptionT(Future.successful(Some("email@example.com"): Option[String])))

      val result = controller.subscriptionCallback(formBundleId)(request)

      verify(mockSubmissionTrackingService, never()).deleteSubmissionTracking(any())
    }

    "delete submission successfully" in {
      val formBundleId = "formBundle123"

      when(mockSubmissionTrackingService.deleteSubmissionTracking(formBundleId))
        .thenAnswer(_ => Future.successful(()))

      val result = controller.deleteSubmission(formBundleId)(FakeRequest())

      status(result) mustBe OK
    }

    "return a message when submission is not found during deletion" in {
      val formBundleId = "formBundle123"

      when(mockSubmissionTrackingService.deleteSubmissionTracking(formBundleId))
        .thenReturn(Future.failed(new NoSuchElementException("Submission not found")))

      val result = controller.deleteSubmission(formBundleId)(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe s"Submission with $formBundleId not found"
    }
  }
}
