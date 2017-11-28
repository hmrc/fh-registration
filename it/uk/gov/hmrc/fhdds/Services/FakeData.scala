package uk.gov.hmrc.fhdds.Services

import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhdds.models.businessregistration.{Address, BusinessRegistrationDetails}
import uk.gov.hmrc.fhdds.models.des.DesSubmissionResponse
import uk.gov.hmrc.fhdds.models.dfsStore.Submission
import uk.gov.hmrc.fhdds.models.fhdds.SubmissionRequest
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraData
import uk.gov.hmrc.fhdds.services.ControllerServices
import uk.gov.hmrc.mongo.CreationAndLastModifiedDetail

import scala.xml.XML

object FakeData {

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

  val fakeBusinessDetails: String = {
    scala.io.Source.fromFile(s"${directoryPath}business-registration-details.json").mkString
  }

  val validFakeSubmissionRef = "ValidSubmissionRef123"

  val testFormTypeRef = "testFormTypeRef"
  val testFormId = "testID"
  val testUserId = "testUserId"

  val aFakeSubmission = Submission(submissionRef = validFakeSubmissionRef,
                                   submissionMark = None,
                                   state = "",
                                   formData = validFormData,
                                   formTypeRef = testFormTypeRef,
                                   formId = testFormId,
                                   casKey = None,
                                   savedDate = None,
                                   submittedDate = None,
                                   userId = "",
                                   expireAt = DateTime.now(),
                                   crudDetail = CreationAndLastModifiedDetail())

  val aFakeInvalidSubmission = Submission(submissionRef = validFakeSubmissionRef,
                                          submissionMark = None,
                                          state = "",
                                          formData = "",
                                          formTypeRef = testFormTypeRef,
                                          formId = testFormId,
                                          casKey = None,
                                          savedDate = None,
                                          submittedDate = None,
                                          userId = "",
                                          expireAt = DateTime.now(),
                                          crudDetail = CreationAndLastModifiedDetail())

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
                                                                                registrationNumberFHDDS = ControllerServices.createSubmissionRef())
}
