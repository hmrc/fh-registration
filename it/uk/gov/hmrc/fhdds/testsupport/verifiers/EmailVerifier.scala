package uk.gov.hmrc.fhdds.testsupport.verifiers

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlPathEqualTo}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._
import uk.gov.hmrc.fhregistration.models.fhdds.SendEmailRequest

case class EmailVerifier()(implicit builder: VerifierBuilder) extends Matchers with VerifierFunctions {


  def emailSent(templateId: String = "fhdds_submission_confirmation") = {
    val path = "/hmrc/email"
    numberOfCalls(postRequestedFor(urlPathEqualTo(path))) shouldBe 1

    val emailRequest = Json.parse(calls(postRequestedFor(urlPathEqualTo(path))).get(0).getBodyAsString).as[SendEmailRequest]

    emailRequest.templateId shouldBe templateId
    emailRequest.to shouldBe List("testUser@email.com")

    builder
  }

}
