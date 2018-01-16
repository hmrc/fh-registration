package uk.gov.hmrc.fhdds.testsupport.preconditions

import com.github.tomakehurst.wiremock.client.WireMock._

case class TaxEnrolmentStub()(implicit builder: PreconditionBuilder) {

  def subscribe(subscriptionId: String) = {

    stubFor(
      post(
        urlPathEqualTo(s"/fhdds/tax-enrolments/subscriptions/$subscriptionId/subscriber")
      )
        .willReturn(
          ok("")
        )
    )
    builder
  }

}
