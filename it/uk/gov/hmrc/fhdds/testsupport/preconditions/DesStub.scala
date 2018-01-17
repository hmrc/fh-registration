package uk.gov.hmrc.fhdds.testsupport.preconditions

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.fhdds.testsupport.TestData
import uk.gov.hmrc.fhregistration.models.des.DesSubmissionResponse
import play.api.libs.json.{JsObject, JsString, JsValue}

case class DesStub()(implicit builder: PreconditionBuilder) {

  import DesSubmissionResponse.submissionResponseFormat

  val receivedJson: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))
  val processingJson: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Sent to RCM")))
  val successfulJson: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Successful")))

  def sendSubmission(safeId: String) = {
    stubFor(
      post(
        urlPathEqualTo(s"/fhdds-stubs/application/$safeId")
      )
        .willReturn(
          ok(
            Json.toJson(TestData.aDesSubmissionResponse).toString
          )
        )
    )
    builder
  }

  def getStatus(registrationID: String) = {
    stubFor(
      get(
        urlPathEqualTo(s"/fhdds-stubs/fulfilment-diligence/subscription/$registrationID/status")
      )
        .willReturn(
          ok(
            registrationID match {
              case "receivedRegistrationNumber" ⇒ receivedJson.toString()
              case "processingRegistrationNumber" ⇒ processingJson.toString()
              case "successfulRegistrationNumber" ⇒ successfulJson.toString()
            }
          )
        )
    )
    builder
  }
}
