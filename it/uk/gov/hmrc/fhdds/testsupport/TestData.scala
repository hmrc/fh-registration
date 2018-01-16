package uk.gov.hmrc.fhdds.testsupport

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhregistration.models.businessregistration.{Address, BusinessRegistrationDetails}
import uk.gov.hmrc.fhregistration.models.des.DesSubmissionResponse
import uk.gov.hmrc.fhregistration.repositories.SubmissionExtraData
import uk.gov.hmrc.fhregistration.services.ControllerServices
import uk.gov.hmrc.fhregistration.models.fhdds.SubmissionRequest

import scala.xml.XML

object TestData {

  val file = "fhdds-limited-company-minimum.xml"
  val directoryPath = s"./it/resources/"


  val validFormData: String = {
    scala.io.Source.fromFile(s"$directoryPath$file").mkString
  }

  val validFormXMLData: generated.limited.Data = {
    val xml = XML
      .load(scala.io.Source.fromFile(s"$directoryPath$file").reader())
    scalaxb.fromXML[generated.limited.Data](xml)
  }

//  val someBusinessDetails: String = {
//    scala.io.Source.fromFile(s"${directoryPath}business-registration-details.json").mkString
//  }

  val validSubmissionRef = "ValidSubmissionRef123"

  val testFormTypeRef = "testFormTypeRef"
  val testFormId = "testFormId"
  val testUserId = "testUserId"
  val testSafeId = "XE0001234567890"

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

  val aSubmissionRequest: SubmissionRequest = SubmissionRequest(formId = testFormId, formTypeRef = testFormTypeRef, formData = validFormData)

  val aDesSubmissionResponse: DesSubmissionResponse = DesSubmissionResponse(
    processingDate = DateTime.now().toString,
    etmpFormBundleNumber = ControllerServices.createSubmissionRef(),
    registrationNumberFHDDS = ControllerServices.createSubmissionRef())
}
