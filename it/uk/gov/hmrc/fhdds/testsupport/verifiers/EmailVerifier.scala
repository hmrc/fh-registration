package uk.gov.hmrc.fhdds.testsupport.verifiers

import org.scalatest.Matchers
import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlPathEqualTo}

case class EmailVerifier()(implicit builder: VerifierBuilder) extends Matchers with VerifierFunctions {


  def emailSent() = {
    val path = "/hmrc/email"
    numberOfCalls(postRequestedFor(urlPathEqualTo(path))) shouldBe 1

    builder
  }

}
