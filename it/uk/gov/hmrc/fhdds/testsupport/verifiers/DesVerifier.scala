package uk.gov.hmrc.fhdds.testsupport.verifiers

import com.github.tomakehurst.wiremock.client.WireMock.{findAll, postRequestedFor, urlMatching}
import org.scalatest.Matchers
import play.api.libs.json.{JsObject, Json}

import scala.collection.JavaConverters._

case class DesVerifier()(implicit builder: VerifierBuilder) extends Matchers {

  def verifiesSubscriptions(): VerifierBuilder = {
    val requests = findAll(postRequestedFor(urlMatching("/fhdds-stubs/fulfilment-diligence/subscription/.*"))).asScala
    requests.size should not be 0
    for (r ← requests) {
      Json.parse(r.getBody).as[JsObject] should not be null
    }

    builder
  }
}
