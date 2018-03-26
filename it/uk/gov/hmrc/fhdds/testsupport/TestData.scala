package uk.gov.hmrc.fhdds.testsupport

import java.util.Date

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.fhregistration.models.des.{DesSubmissionResponse, DesWithdrawalResponse}
import uk.gov.hmrc.fhregistration.models.fhdds.SubmissionRequest

object TestData {

  val directoryPath = s"./it/resources/"

  val validNewFormData: JsValue = {
    Json.parse(scala.io.Source.fromFile(s"${directoryPath}fhdds-limited-company-large-uk.json").mkString)
  }

  val validAmendFormData: JsValue = {
    Json.parse(scala.io.Source.fromFile(s"${directoryPath}fhdds-limited-company-large-uk-amendment.json").mkString)
  }

  val testUserEmail = "testUser@email.com"
  val testSafeId = "XE0001234567890"
  val testRegistrationNumber = "XE0001234567890"
  val testEtmpFormBundleNumber: String = Array.fill(9)((math.random * 10).toInt).mkString

  val testWithdrawalBody =
    s"""{"emailAddress": "$testUserEmail", "withdrawal": {"withdrawalDate": "2017-11-29","withdrawalReason": "Applied in Error"}}"""

  val testInvalidWithdrawalBody =
    s"""{"emailAddress": "$testUserEmail"}"""

  val validSubmissionRequest: SubmissionRequest = SubmissionRequest(testUserEmail, validNewFormData)

  val validAmendSubmissionRequest: SubmissionRequest = SubmissionRequest(testUserEmail, validAmendFormData)

  def desSubmissionResponse(etmpFormBundleNumber: String, registrationNumberFHDDS: String): DesSubmissionResponse = {
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
