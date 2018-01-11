package uk.gov.hmrc.fhregistration.services

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.OneAppPerTest
import play.api.http.HttpEntity
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.fhregistration.connectors._
import uk.gov.hmrc.fhregistration.controllers.FhddsApplicationController
import uk.gov.hmrc.fhregistration.repositories.{SubmissionExtraData, SubmissionExtraDataRepository}
import uk.gov.hmrc.fhregistration.services.FakeData.aFakeSubmissionRequest
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait FhddsApplicationIntegrationMocks extends FeatureSpec with GivenWhenThen with ScalaFutures
  with Matchers with BeforeAndAfterEach with MockitoSugar with MongoSpecSupport with OneAppPerTest {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(10, Millis))

  implicit val reactiveMongoComponent: ReactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }

  var mockDesConnector: DesConnector = mock[DesConnectorImpl]
  var mockTaxEnrolmentConnector: TaxEnrolmentConnector = mock[TaxEnrolmentConnectorImpl]
  var mockSubmissionExtraDataRepository: SubmissionExtraDataRepository = mock[SubmissionExtraDataRepository]
  var mockFhddsApplicationService: FhddsApplicationService = new FhddsApplicationServiceImpl(new CountryCodeLookupImpl)
  var auditService: AuditService = new AuditServiceImpl

  var fhddsApplicationController = new FhddsApplicationController(
    mockDesConnector,
    mockTaxEnrolmentConnector,
    mockSubmissionExtraDataRepository,
    mockFhddsApplicationService,
    auditService
  )

  override def beforeEach(): Unit = {
    mockDesConnector = mock[DesConnectorImpl]
    mockTaxEnrolmentConnector = mock[TaxEnrolmentConnectorImpl]
    mockSubmissionExtraDataRepository = mock[SubmissionExtraDataRepository]
    mockFhddsApplicationService = new FhddsApplicationServiceImpl(new CountryCodeLookupImpl)

    fhddsApplicationController = new FhddsApplicationController(
      mockDesConnector,
      mockTaxEnrolmentConnector,
      mockSubmissionExtraDataRepository,
      mockFhddsApplicationService,
      auditService)
  }

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val fakePostRequest = FakeRequest("POST", "/application/submit", FakeHeaders(), body = aFakeSubmissionRequest)

  override def afterEach {
    mongo.apply().drop.futureValue
  }

  def createDesSubmission(formData: String, extraData: SubmissionExtraData) = {
    val xml = scala.xml.XML.loadString(formData)
    val data = scalaxb.fromXML[generated.limited.Data](xml)
    mockFhddsApplicationService.iformXmlToApplication(data, extraData.businessRegistrationDetails)
  }

  def consume(data: HttpEntity)(implicit ec: ExecutionContext): String = {
    implicit val system = ActorSystem()
    implicit val materializer: Materializer = ActorMaterializer()
    val bytes = Await.result(data.consumeData, 500.millis)
    bytes.utf8String
  }
}
