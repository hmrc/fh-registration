package uk.gov.hmrc.fhdds.testsupport

import java.util.{Date, UUID}

import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.fhregistration.models.des.{DesSubmissionResponse, DesWithdrawalResponse}
import uk.gov.hmrc.fhregistration.models.fhdds.SubmissionRequest


object TestData {

  val file = "fhdds-limited-company-minimum.json"

  val directoryPath = s"./it/resources/"

  val validNewFormData: JsValue = {
    Json.parse(scala.io.Source.fromFile(s"${directoryPath}fhdds-limited-company-large-uk.json").mkString)
  }

  val validAmendFormData: JsValue = {
    Json.parse(scala.io.Source.fromFile(s"${directoryPath}fhdds-limited-company-large-uk-amendment.json").mkString)
  }

  val testUserEmail = "testUser@email.com"
//  val someBusinessDetails: String = {
//    scala.io.Source.fromFile(s"${directoryPath}business-registration-details.json").mkString
//  }

  val validSubmissionRef = "ValidSubmissionRef123"

  val testFormTypeRef = "testFormTypeRef"
  val testFormId = "testFormId"
  val testUserId = "test-" + UUID.randomUUID()
  val testSafeId = "XE0001234567890"
  val testRegistrationNumber = "XEFH0001234567890"
  val anotherRegistrationNumber = "XEFH0001234567891"
  val testEtmpFormBundleNumber: String = Array.fill(9)((math.random * 10).toInt).mkString
  val validFormData: String = {
    scala.io.Source.fromFile(s"$directoryPath$file").mkString
  }
  val testWithdrawalBody =
    s"""{"emailAddress": "$testUserEmail", "withdrawal": {"withdrawalDate": "2017-11-29","withdrawalReason": "Applied in Error"}}"""

  val testInvalidWithdrawalBody =
    s"""{"emailAddress": "$testUserEmail"}"""

  val validSubmissionRequest: SubmissionRequest = SubmissionRequest(testUserEmail, validNewFormData)

  val validAmendSubmissionRequest: SubmissionRequest = SubmissionRequest(testUserEmail, validAmendFormData)

  val someTaxEnrolmentResponse: JsObject = Json.obj(
    "serviceName" → JsString("serviceName"),
    "callback" → JsString("callback"),
    "etmpId" → JsString("etmpId"))

  val aSubmissionRequest: SubmissionRequest = SubmissionRequest(
    emailAddress = "a@a.test",
    submission = Json.parse(validFormData))

  def desSubmissionResponse(etmpFormBundleNumber: String, registrationNumberFHDDS: String) = {
    DesSubmissionResponse(
      processingDate = new Date(),
      etmpFormBundleNumber = etmpFormBundleNumber,
      registrationNumberFHDDS = registrationNumberFHDDS)
  }

  def desWithdrawalResponse: DesWithdrawalResponse = {
    DesWithdrawalResponse(
      processingDate = new Date()
    )
  }

}
