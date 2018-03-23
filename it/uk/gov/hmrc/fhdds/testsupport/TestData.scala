package uk.gov.hmrc.fhdds.testsupport

import java.util.Date

import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.fhregistration.models.businessregistration.{Address, BusinessRegistrationDetails}
import uk.gov.hmrc.fhregistration.models.des.{DesSubmissionResponse, DesWithdrawalResponse}
import uk.gov.hmrc.fhregistration.models.fhdds.SubmissionRequest
import uk.gov.hmrc.fhregistration.repositories.SubmissionExtraData

object TestData {

  val file = "fhdds-limited-company-large-uk.json"
  val directoryPath = s"./it/resources/"

  val validFormData: JsValue = {
    Json.parse(scala.io.Source.fromFile(s"$directoryPath$file").mkString)
  }

  val validSubmissionRef = "ValidSubmissionRef123"

  val testFormTypeRef = "testFormTypeRef"
  val testFormId = "testFormId"
  val testUserId = "testUserId"
  val testUserEmail = "testUser@email.com"
  val testSafeId = "XE0001234567890"
  val testRegistrationNumber = "XE0001234567890"

  val invalidUserId = "invalidUserId"
  val invalidFormTypeRef = "invalidFormTypeRef"

  val fakeBusinessDetailsJson: String =
    s"""
       |{
       |  "business_name":"Real Business Inc",
       |  "business_type":"corporate body",
       |  "business_address":{
       |    "line1":"line1",
       |    "line2":"line2",
       |    "postcode":"NE98 1ZZ",
       |    "country":"GB"
       |  },
       |  "sap_number":"1234567890",
       |  "safe_id":"XE0001234567890",
       |  "is_a_group":false,
       |  "direct_match":false,
       |  "agent_reference_number":"JARN1234567",
       |  "utr":"1111111111",
       |  "is_business_details_editable":false
       |  }
     """.stripMargin

  val testWithdrawalBody =
    s"""{"emailAddress": "$testUserEmail", "withdrawal": {"withdrawalDate": "2017-11-29","withdrawalReason": "Applied in Error"}}"""

  val testInvalidWithdrawalBody =
    s"""{"emailAddress": "$testUserEmail"}"""

  val anAddress = Address(
    line1 = "line1",
    line2 = "line2",
    line3 = None,
    line4 = None,
    postcode = Some("NE98 1ZZ"),
    country = "GB")

  val someBusinessRegistrationDetails = BusinessRegistrationDetails(
    businessName = "Real Business Inc",
    businessType = Some("corporate body"),
    businessAddress = anAddress,
    sapNumber = "1234567890",
    safeId = s"$testSafeId",
    agentReferenceNumber = Some("JARN1234567"),
    firstName = None,
    lastName = None,
    utr = Some("1111111111"),
    identification = None)

  val someSubmissionExtraData = SubmissionExtraData(
    encUserId = "",
    formTypeRef = testFormTypeRef,
    formId = Some(testFormId),
    submissionRef = None,
    registrationNumber = None,
    businessRegistrationDetails = someBusinessRegistrationDetails,
    companyRegistrationNumber = None,
    authorization = None)

  val someTaxEnrolmentResponse: JsObject = Json.obj(
    "serviceName" → JsString("serviceName"),
    "callback" → JsString("callback"),
    "etmpId" → JsString("etmpId"))

  val aSubmissionRequest: SubmissionRequest = SubmissionRequest(testUserEmail, validFormData)

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
