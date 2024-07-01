package uk.gov.hmrc.fhdds.testsupport.verifiers

import com.github.tomakehurst.wiremock.client.WireMock.findAll
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder

trait VerifierFunctions {

  def calls(r: RequestPatternBuilder) =
    findAll(r)

  def numberOfCalls(r: RequestPatternBuilder) =
    calls(r).size()

}
