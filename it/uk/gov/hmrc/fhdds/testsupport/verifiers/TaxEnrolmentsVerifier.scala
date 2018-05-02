package uk.gov.hmrc.fhdds.testsupport.verifiers

import com.github.tomakehurst.wiremock.client.WireMock.{deleteRequestedFor, urlPathEqualTo}
import org.scalatest.Matchers


case class TaxEnrolmentsVerifier()(implicit builder: VerifierBuilder) extends Matchers with VerifierFunctions {

  def deEnrolmentCalled(groupId: String, registrationNumber: String) = {
    val path = s"/tax-enrolments/groups/$groupId/enrolments/HMRC-OBTDS-ORG~ETMPREGISTRATIONNUMBER~$registrationNumber"

    numberOfCalls(deleteRequestedFor(urlPathEqualTo(path))) shouldBe 1

    builder
  }

}
