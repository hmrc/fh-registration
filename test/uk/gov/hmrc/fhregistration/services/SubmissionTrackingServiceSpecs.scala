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

package uk.gov.hmrc.fhregistration.services

import java.time.Clock

import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{reset, when}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress
import uk.gov.hmrc.fhregistration.repositories.DefaultSubmissionTrackingRepository
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionTrackingServiceSpecs extends UnitSpec
  with ScalaFutures
  with MockitoSugar
  with BeforeAndAfterEach
  with MongoSpecSupport
  with Matchers {


  implicit val reactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector = mongoConnectorForTest
  }

  val repository = new DefaultSubmissionTrackingRepository()

  override protected def beforeEach(): Unit = {
    await(repository.drop)
    await(repository.ensureIndexes)
    reset(mockClock)
    when(mockClock.millis()).thenReturn(System.currentTimeMillis())
  }

  val mockClock = mock[Clock]

  val service = new DefaultSubmissionTrackingService(repository, mockClock)

  "enrolmentProgress" should {
    "be Unknown" in {
      await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Unknown
    }

    "be Pending" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co"))
      await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Pending
    }

    "be Error" when {
      "the tracking was marked as error" in {
        await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co"))
        await(service.updateSubscriptionTracking("formbundelid", EnrolmentProgress.Error))
        await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Error
      }

      "the pending tracking was not updated" in {
        await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co"))
        when(mockClock.millis()).thenReturn(System.currentTimeMillis() + 2 * service.SubmissionTrackingAgeThresholdMs )
        await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Error
      }
    }

  }

  "saveSubscriptionTracking" should {
    "overwrite a previous tracking" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co"))
      await(service.updateSubscriptionTracking("formbundelid", EnrolmentProgress.Error))
      await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Error

      await(service.saveSubscriptionTracking("safeid", "some-user", "another-formbundelid", "a@a.co"))

      await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Pending
    }
  }

  "deleteSubmissionTracking" should {
    "remove the tracking so the progress is now Unknown" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co"))
      await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Pending
      await(service.deleteSubmissionTracking("formbundelid"))
      await(service.enrolmentProgress("some-user")) shouldBe EnrolmentProgress.Unknown
    }
  }




}