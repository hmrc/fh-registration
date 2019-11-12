/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.fhregistration.repositories

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress
import uk.gov.hmrc.fhregistration.util.LogCapturing
import uk.gov.hmrc.mongo.{Awaiting, CurrentTime, MongoSpecSupport}

class SubmissionTrackingRepositorySpecs
    extends WordSpec with Matchers with MongoSpecSupport with BeforeAndAfterEach with Awaiting with CurrentTime
    with Eventually with LogCapturing {

  implicit val reactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector = mongoConnectorForTest
  }

  val repository = new DefaultSubmissionTrackingRepository()

  override protected def beforeEach(): Unit = {
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Inserting a new record" should {
    "Be successful" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId)).get
      byUserId shouldEqual tracking

      val byFormBunldeId = await(repository.findSubmissionTrakingByFormBundleId(aFormBundleId)).get
      byFormBunldeId shouldEqual tracking

    }

  }

  "Deleted a record" should {
    "be successful" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val nDeleted = await(repository.deleteSubmissionTackingByFormBundleId(aFormBundleId))
      nDeleted shouldBe 1

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId))

      byUserId shouldBe None

      val nDeletedZero = await(repository.deleteSubmissionTackingByFormBundleId(aFormBundleId))
      nDeletedZero shouldBe 0
    }
  }

  "Delete a record by registration with fhreg number" should {
    "delete the registration" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val nDeleted =
        await(repository.deleteSubmissionTackingByRegistrationNumber(tracking.userId, tracking.registrationNumber.get))
      nDeleted shouldBe 1

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId))

      byUserId shouldBe None
    }

    "don't delete it if it does not match" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val nDeleted = await(
        repository.deleteSubmissionTackingByRegistrationNumber(tracking.userId, tracking.registrationNumber.get + "1"))
      nDeleted shouldBe 0

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId))

      byUserId shouldBe Some(tracking)
    }

  }

  "enrolmentProgress" should {
    "Return pending if None" in {
      val testSubmissionTracking = SubmissionTracking("", "", "", 0, None, None)

      testSubmissionTracking.enrolmentProgress shouldBe EnrolmentProgress.Pending
    }
  }

  def mkSubmissionTracking = SubmissionTracking(
    anUserId,
    aFormBundleId,
    anEmail,
    System.currentTimeMillis(),
    EnrolmentProgress.Pending,
    "XXFH00000123432"
  )

  val anUserId = "userid-1"
  val unknownUserId = "userid-u"
  val aFormBundleId: String = "012345678901"
  val anEmail = "test@test.com"

}
