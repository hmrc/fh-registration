package uk.gov.hmrc.fhdds.testsupport

import com.github.tomakehurst.wiremock.WireMockServer
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.fhdds.testsupport.preconditions.PreconditionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{configureFor, reset, resetAllScenarios}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import uk.gov.hmrc.fhdds.testsupport.verifiers.VerifierBuilder

trait TestConfiguration
  extends GuiceOneServerPerSuite
    with IntegrationPatience
    with PatienceConfiguration
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  me: Suite with TestSuite =>

  val wiremockHost: String = "localhost"
  val wiremockPort: Int = Port.randomAvailable

  abstract override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = Span(4, Seconds),
      interval = Span(50, Millis))

  def given() = new PreconditionBuilder

  def expect() = new VerifierBuilder

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(replaceWithWiremock(Seq(
      "auth",
      "des-service",
      "fhdds",
      "tax-enrolments",
      "email"
    )))
    .build()


  private def replaceWithWiremock(services: Seq[String]) =
    services.foldLeft(Map.empty[String, Any]) { (configMap, service) =>
      configMap + (
        s"microservice.services.$service.host" -> wiremockHost,
        s"microservice.services.$service.port" -> wiremockPort)
    } +
      (s"auditing.consumer.baseUri.host" -> wiremockHost, s"auditing.consumer.baseUri.port" -> wiremockPort)

  val wireMockServer = new WireMockServer(wireMockConfig().port(wiremockPort))

  override def beforeAll() = {
    wireMockServer.stop()
    wireMockServer.start()
    configureFor(wiremockHost, wiremockPort)
  }

  override def beforeEach() = {

    resetAllScenarios()
    reset()
  }

  override protected def afterAll(): Unit = {
    wireMockServer.stop()
  }
}
