package uk.gov.hmrc.fhdds.testsupport.preconditions


import com.github.tomakehurst.wiremock.client.WireMock._

case class EmailStub()(implicit builder: PreconditionBuilder) {

  def sendEmail = {
    stubFor(
      post(
        urlPathEqualTo(s"/hmrc/email")
      )
        .willReturn(
          ok()
        )
    )
    builder
  }

}
