package uk.gov.hmrc.fhregistration.services

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhregistration.models.businessregistration.{Address, BusinessRegistrationDetails}
import uk.gov.hmrc.fhregistration.models.des.DesSubmissionResponse
import uk.gov.hmrc.fhregistration.models.fhdds.SubmissionRequest
import uk.gov.hmrc.fhregistration.repositories.SubmissionExtraData

import scala.xml.XML

object FakeData {

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

  val fakeBusinessDetails: String = {
    scala.io.Source.fromFile(s"${directoryPath}business-registration-details.json").mkString
  }

  val validFakeSubmissionRef = "ValidSubmissionRef123"

  val testFormTypeRef = "testFormTypeRef"
  val testFormId = "testID"
  val testUserId = "testUserId"

  val testETMPFormBundleNumber: String = "012345678901"

  val aFakeAddress = Address(line1 = "line1",
                             line2 = "line2",
                             line3 = None,
                             line4 = None,
                             postcode = Some("NE98 1ZZ"),
                             country = "GB")

  val aFakeBusinessRegistrationDetails = BusinessRegistrationDetails(businessName = "Real Business Inc",
                                                                     businessType = Some("corporate body"),
                                                                     businessAddress = aFakeAddress,
                                                                     sapNumber = "1234567890",
                                                                     safeId = "XE0001234567890",
                                                                     agentReferenceNumber = Some("JARN1234567"),
                                                                     firstName = None,
                                                                     lastName = None,
                                                                     utr = Some("1111111111"),
                                                                     identification = None)

  val aFakeSubmissionExtraData = SubmissionExtraData(encUserId = "",
                                                     formTypeRef = testFormTypeRef,
                                                     formId = Some(testFormId),
                                                     submissionRef = None,
                                                     registrationNumber = None,
                                                     businessRegistrationDetails = aFakeBusinessRegistrationDetails,
                                                     companyRegistrationNumber = None,
                                                     authorization = None)

  val aFakeJsonObject: JsObject = Json.obj("serviceName" → JsString("serviceName"),
                                           "callback" → JsString("callback"),
                                           "etmpId" → JsString("etmpId"))

  val aFakeSubmissionRequest: SubmissionRequest = SubmissionRequest(formId = testFormId, formTypeRef = testFormTypeRef, formData = validFormData)

  val aFakeDesSubmissionResponse: DesSubmissionResponse = DesSubmissionResponse(processingDate = DateTime.now().toString,
                                                                                etmpFormBundleNumber = testETMPFormBundleNumber,
                                                                                registrationNumberFHDDS = ControllerServices.createSubmissionRef())
}
