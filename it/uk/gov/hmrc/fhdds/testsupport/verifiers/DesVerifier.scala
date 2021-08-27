package uk.gov.hmrc.fhdds.testsupport.verifiers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.matchers.should.Matchers
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

  def withdrawalCalled(registrationNumber: String): VerifierBuilder = {
    val path = s"/fhdds-stubs/fulfilment-diligence/subscription/$registrationNumber/withdrawal"
    val requests = findAll(putRequestedFor(urlPathEqualTo(path)))

    requests.size shouldBe 1

    builder
  }
}
