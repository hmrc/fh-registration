package uk.gov.hmrc.fhdds.testsupport.preconditions


import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import uk.gov.hmrc.fhdds.testsupport.TestData
import uk.gov.hmrc.fhregistration.models.des.DesSubmissionResponse


case class DesStub()(implicit builder: PreconditionBuilder) {

  import DesSubmissionResponse.submissionResponseFormat

  val receivedJson: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Reg Form Received")))
  val processingJson: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Sent to RCM")))
  val successfulJson: JsValue = JsObject(Seq("subscriptionStatus" → JsString("Successful")))

  def acceptsSubscription(safeId: String, registrationNumber: String, etmpFormNumberBundle: String) = {
    stubFor(
      post(
        urlPathEqualTo(s"/fhdds-stubs/fulfilment-diligence/subscription/id/$safeId/id-type/safe")
      )
        .willReturn(
          ok(
            Json.toJson(TestData.desSubmissionResponse(etmpFormNumberBundle, registrationNumber)).toString
          )
        )
    )
    builder
  }

  def acceptsWithdrawal(registrationNumber: String) = {
    stubFor(
      put(
        urlPathEqualTo(s"/fhdds-stubs/fulfilment-diligence/subscription/$registrationNumber/withdrawal")
      )
        .willReturn(
          ok(
            s"""{"processingDate": "2018-12-17T09:30:47Z"}"""
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
