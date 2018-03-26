package uk.gov.hmrc.fhdds.testsupport.preconditions

import com.github.tomakehurst.wiremock.client.WireMock._

case class TaxEnrolmentStub()(implicit builder: PreconditionBuilder) {

  def subscribe = {
    stubFor(
      put(
        urlPathEqualTo(s"/tax-enrolments/callback/subscriptions")
      )
        .willReturn(
          ok("{}")
        )
    )
    builder
  }

}
