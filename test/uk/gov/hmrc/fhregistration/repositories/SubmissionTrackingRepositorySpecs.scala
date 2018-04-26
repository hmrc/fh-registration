/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatest.concurrent.Eventually
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.crypto.CompositeSymmetricCrypto
import uk.gov.hmrc.mongo.{Awaiting, CurrentTime, MongoSpecSupport}
import uk.gov.hmrc.play.test.LogCapturing

class SubmissionTrackingRepositorySpecs
  extends WordSpec
    with Matchers
    with MongoSpecSupport
    with BeforeAndAfterEach
    with Awaiting
    with CurrentTime
    with Eventually
    with LogCapturing {

  implicit val reactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector = mongoConnectorForTest
  }

  implicit val crypto = CompositeSymmetricCrypto.aes("962D3D205B9E29A74D25D0743B1C11E0", Seq.empty)
  val repository = new SubmissionTrackingRepository()


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

  def mkSubmissionTracking = SubmissionTracking(
    anUserId,
    aFormBundleId,
    anEmail,
    System.currentTimeMillis()
  )


  val anUserId = "userid-1"
  val unknownUserId = "userid-u"
  val aFormBundleId: String = "012345678901"
  val anEmail = "test@test.com"

}
