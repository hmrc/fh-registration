package uk.gov.hmrc.fhdds.testsupport.preconditions

import com.github.tomakehurst.wiremock.client.WireMock._

case class TaxEnrolmentStub()(implicit builder: PreconditionBuilder) {

  def subscribe = {
    stubFor(
      put(
        urlMatching(s"/tax-enrolments/subscriptions/([0-9a-zA-Z]+)/subscriber")
      )
        .willReturn(
          ok("{}")
        )
    )
    builder
  }

  def acceptsDeEnrolment() = {
    stubFor(
      delete(
        urlMatching("/tax-enrolments/groups/([0-9a-zA-Z\\-~]+)/enrolments/([0-9a-zA-Z\\-~]+)"))
      .willReturn((noContent()))
    )

    builder
  }

}
