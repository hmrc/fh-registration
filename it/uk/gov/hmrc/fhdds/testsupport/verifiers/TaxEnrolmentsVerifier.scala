package uk.gov.hmrc.fhdds.testsupport.verifiers

import com.github.tomakehurst.wiremock.client.WireMock.{deleteRequestedFor, putRequestedFor, urlMatching, urlPathEqualTo}
import org.scalatest.matchers.should.Matchers

case class TaxEnrolmentsVerifier()(implicit builder: VerifierBuilder) extends Matchers with VerifierFunctions {

  def deEnrolmentCalled(groupId: String, registrationNumber: String) = {
    val path = s"/tax-enrolments/groups/$groupId/enrolments/HMRC-OBTDS-ORG~ETMPREGISTRATIONNUMBER~$registrationNumber"

    numberOfCalls(deleteRequestedFor(urlPathEqualTo(path))) shouldBe 1

    builder
  }

  def noDeEnrolmentCalled() = {
    val path = s"/tax-enrolments/groups/.+"

    numberOfCalls(deleteRequestedFor(urlMatching(path))) shouldBe 0

    builder

  }

  def enrolmentCalled(registrationNumber: String, formBundleId: String) = {
    val path = s"/tax-enrolments/subscriptions/$formBundleId/subscriber"

    numberOfCalls(putRequestedFor(urlMatching(path))) shouldBe 1

    builder
  }
}
