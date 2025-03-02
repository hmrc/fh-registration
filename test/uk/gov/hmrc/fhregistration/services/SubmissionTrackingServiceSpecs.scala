/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress
import uk.gov.hmrc.fhregistration.repositories.SubmissionTrackingRepository
import uk.gov.hmrc.fhregistration.util.UnitSpec

import java.time.Clock
import scala.concurrent.Future

class SubmissionTrackingServiceSpecs extends UnitSpec with ScalaFutures with MockitoSugar with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    repository.collection.drop()
    reset(mockClock)
    when(mockClock.millis()).thenReturn(System.currentTimeMillis())
  }

  val mockClock = mock[Clock]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  val service = new DefaultSubmissionTrackingService(repository, mockClock)

  "enrolmentProgress" should {
    "be Unknown" in {
      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Unknown
    }

    "be Pending" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Pending
    }

    "be Unknown when the user has the registration number" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.enrolmentProgress("some-user", Some("ZZFH0000001231456"))) shouldBe EnrolmentProgress.Unknown
    }

    "be Pending when the registration number does not match" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.enrolmentProgress("some-user", Some("ZZFH0000008888888"))) shouldBe EnrolmentProgress.Pending
    }

    "be Error" when {
      "the tracking was marked as error" in {
        await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
        await(service.updateSubscriptionTracking("formbundelid", EnrolmentProgress.Error))
        await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Error
      }

      "the pending tracking was not updated" in {
        await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
        when(mockClock.millis()).thenReturn(System.currentTimeMillis() + 2 * service.SubmissionTrackingAgeThresholdMs)
        await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Error
      }
    }
  }

  "saveSubscriptionTracking" should {
    "overwrite a previous tracking" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.updateSubscriptionTracking("formbundelid", EnrolmentProgress.Error))
      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Error

      await(
        service.saveSubscriptionTracking("safeid", "some-user", "another-formbundelid", "a@a.co", "ZZFH0000001231456")
      )

      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Pending
    }
  }

  "deleteSubmissionTracking" should {
    "remove the tracking so the progress is now Unknown" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Pending
      await(service.deleteSubmissionTracking("formbundelid"))
      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Unknown
    }
  }

  "enrolmentProgress" should {
    "remove tracking when registration number matches" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.enrolmentProgress("some-user", Some("ZZFH0000001231456"))) shouldBe EnrolmentProgress.Unknown
    }

    "retain tracking when registration number does not match" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      await(service.enrolmentProgress("some-user", Some("ZZFH0000008888888"))) shouldBe EnrolmentProgress.Pending
    }
  }

  "getSubmissionTrackingEmail" should {
    "return the email for an existing form bundle ID" in {
      await(service.saveSubscriptionTracking("safeid", "some-user", "formbundelid", "a@a.co", "ZZFH0000001231456"))
      val email = service.getSubmissionTrackingEmail("formbundelid").value.futureValue
      email shouldBe Some("a@a.co")
    }

    "return None for a non-existing form bundle ID" in {
      val email = service.getSubmissionTrackingEmail("nonexistent-formbundelid").value.futureValue
      email shouldBe None
    }
  }

  "deleteSubmissionTracking" should {
    "log a warning for non-existing form bundle IDs" in {
      await(service.deleteSubmissionTracking("formbundelid"))
      await(service.deleteSubmissionTracking("nonexistent-formbundelid"))

      await(service.deleteSubmissionTracking("nonexistent-formbundelid"))
      await(service.enrolmentProgress("some-user", None)) shouldBe EnrolmentProgress.Unknown
    }

    "log an error for unexpected delete results" in {
      val mockRepository = mock[SubmissionTrackingRepository]
      when(mockRepository.deleteSubmissionTackingByFormBundleId("formbundelid"))
        .thenReturn(Future.successful(2))

      val serviceWithMockRepo = new DefaultSubmissionTrackingService(mockRepository, mockClock)
      await(serviceWithMockRepo.deleteSubmissionTracking("formbundelid"))
    }

  }

}
