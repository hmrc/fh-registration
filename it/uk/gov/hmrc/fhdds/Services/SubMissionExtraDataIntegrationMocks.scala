package uk.gov.hmrc.fhdds.Services

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.OneAppPerTest
import play.api.http.HttpEntity
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.fhdds.Services.FakeData.aFakeBusinessRegistrationDetails
import uk.gov.hmrc.fhdds.controllers.SubmissionExtraDataController
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraDataRepository
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

trait SubMissionExtraDataIntegrationMocks extends FeatureSpec with GivenWhenThen with ScalaFutures
  with Matchers with BeforeAndAfterEach with MockitoSugar with MongoSpecSupport with OneAppPerTest {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(10, Millis))

  implicit val reactiveMongoComponent: ReactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  }

  var mockSubmissionExtraDataRepository: SubmissionExtraDataRepository = new SubmissionExtraDataRepository


  var submissionExtraDataController = new SubmissionExtraDataController(mockSubmissionExtraDataRepository)

  override def beforeEach(): Unit = {
    submissionExtraDataController = new SubmissionExtraDataController(mockSubmissionExtraDataRepository)
  }

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val fakePutRequest = FakeRequest("PUT", "/submission-extra-data/:userId/:formTypeRef/businessRegistrationDetails",
                                   FakeHeaders(), body = aFakeBusinessRegistrationDetails)

  val fakePutRequestForUpdateFormId = FakeRequest("PUT", "/submission-extra-data/:userId/:formTypeRef/formId",
                                                  FakeHeaders(), body = "test123")

  override def afterEach {
    mongo.apply().drop.futureValue
  }

  def consume(data: HttpEntity)(implicit ec: ExecutionContext): Array[Byte] = {
    implicit val system = ActorSystem()
    implicit val materializer: Materializer = ActorMaterializer()
    Await.result(data.consumeData, 500.millis).toArray
  }

}
