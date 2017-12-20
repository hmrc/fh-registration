package uk.gov.hmrc.fhdds.testsupport

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhdds.models.businessregistration.{Address, BusinessRegistrationDetails}
import uk.gov.hmrc.fhdds.models.des.DesSubmissionResponse
import uk.gov.hmrc.fhdds.models.fhdds.SubmissionRequest
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraData
import uk.gov.hmrc.fhdds.services.ControllerServices

import scala.xml.XML

object TestData {

  val file = "fhdds-limited-company-minimum.xml"
  val directoryPath = s"./it/resources/"


  val validFormData: String = {
    scala.io.Source.fromFile(s"$directoryPath$file").mkString
  }

  val validFormXMLData: generated.Data = {
    val xml = XML
      .load(scala.io.Source.fromFile(s"$directoryPath$file").reader())
    scalaxb.fromXML[generated.Data](xml)
  }

  val someBusinessDetails: String = {
    scala.io.Source.fromFile(s"${directoryPath}business-registration-details.json").mkString
  }

  val validSubmissionRef = "ValidSubmissionRef123"

  val testFormTypeRef = "testFormTypeRef"
  val testFormId = "testID"
  val testUserId = "testUserId"

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
    safeId = "XE0001234567890",
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
    registrationNumberFHDDS = ControllerServices.createSubmissionRef())
}
