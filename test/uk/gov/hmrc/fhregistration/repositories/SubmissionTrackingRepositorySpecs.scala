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

package uk.gov.hmrc.fhregistration.repositories

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress
import uk.gov.hmrc.fhregistration.repositories.SubmissionTrackingRepositorySpecs._
import uk.gov.hmrc.fhregistration.util.UnitSpec

class SubmissionTrackingRepositorySpecs extends UnitSpec with BeforeAndAfterAll with ScalaFutures {

  override protected def beforeAll(): Unit =
    repository.collection.drop()

  "Inserting a new record" should {
    "Be successful" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId)).get
      byUserId shouldEqual tracking

      val byFormBundleId = await(repository.findSubmissionTrackingByFormBundleId(aFormBundleId)).get
      byFormBundleId shouldEqual tracking
    }

    "Overwrite an existing record if userId matches" in {
      val tracking1 = mkSubmissionTracking.copy(formBundleId = "form1")
      val tracking2 = mkSubmissionTracking.copy(formBundleId = "form2", email = "new@test.com")

      await(repository.insertSubmissionTracking(tracking1))
      await(repository.insertSubmissionTracking(tracking2)) // This should overwrite tracking1

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId)).get
      byUserId.formBundleId shouldBe "form2"
      byUserId.email shouldBe "new@test.com"
    }
  }

  "Deleted a record" should {
    "Be successful" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val nDeleted = await(repository.deleteSubmissionTackingByFormBundleId(aFormBundleId))
      nDeleted shouldBe 1

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId))
      byUserId shouldBe None

      val nDeletedZero = await(repository.deleteSubmissionTackingByFormBundleId(aFormBundleId))
      nDeletedZero shouldBe 0
    }

    "Do nothing if the formBundleId does not exist" in {
      val nDeleted = await(repository.deleteSubmissionTackingByFormBundleId("nonexistent-form"))
      nDeleted shouldBe 0
    }
  }

  "Delete a record by registration with fhreg number" should {
    "Delete the registration successfully" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val nDeleted =
        await(repository.deleteSubmissionTackingByRegistrationNumber(tracking.userId, tracking.registrationNumber.get))
      nDeleted shouldBe 1

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId))
      byUserId shouldBe None
    }

    "Do nothing if registration number does not match" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val nDeleted = await(
        repository.deleteSubmissionTackingByRegistrationNumber(tracking.userId, tracking.registrationNumber.get + "1")
      )
      nDeleted shouldBe 0

      val byUserId = await(repository.findSubmissionTrackingByUserId(anUserId))
      byUserId shouldBe Some(tracking)
    }
  }

  "Find all records" should {
    "Return all stored submission tracking records" in {
      val tracking1 = mkSubmissionTracking.copy(formBundleId = "form1")
      val tracking2 = mkSubmissionTracking.copy(formBundleId = "form2", userId = "userid-2")

      await(repository.insertSubmissionTracking(tracking1))
      await(repository.insertSubmissionTracking(tracking2))

      val allRecords = await(repository.findAll())
      allRecords should contain theSameElementsAs List(tracking1, tracking2)
    }
  }

  "Update enrolment progress" should {
    "Update the progress for an existing formBundleId" in {
      val tracking = mkSubmissionTracking
      await(repository.insertSubmissionTracking(tracking))

      val updatedRecords = await(repository.updateEnrolmentProgress(aFormBundleId, EnrolmentProgress.Error))
      updatedRecords.size shouldBe 1

      val byFormBundleId = await(repository.findSubmissionTrackingByFormBundleId(aFormBundleId)).get
      byFormBundleId.enrolmentProgress shouldBe EnrolmentProgress.Error
    }

    "Do nothing if the formBundleId does not exist" in {
      val updatedRecords = await(repository.updateEnrolmentProgress("nonexistent-form", EnrolmentProgress.Error))
      updatedRecords shouldBe empty
    }
  }

  "Find submission tracking by userId" should {
    "Return None if the userId does not exist" in {
      val result = await(repository.findSubmissionTrackingByUserId(unknownUserId))
      result shouldBe None
    }
  }

  "Find submission tracking by formBundleId" should {
    "Return None if the formBundleId does not exist" in {
      val result = await(repository.findSubmissionTrackingByFormBundleId("nonexistent-form"))
      result shouldBe None
    }
  }

  "enrolmentProgress" should {
    "Return pending if None" in {
      val testSubmissionTracking = SubmissionTracking("", "", "", 0, None, None)
      testSubmissionTracking.enrolmentProgress shouldBe EnrolmentProgress.Pending
    }
  }
}

object SubmissionTrackingRepositorySpecs {
  def mkSubmissionTracking: SubmissionTracking = SubmissionTracking(
    userId = anUserId,
    formBundleId = aFormBundleId,
    email = anEmail,
    submissionTime = System.currentTimeMillis(),
    enrolmentProgress = EnrolmentProgress.Pending,
    registrationNumber = "XXFH00000123432"
  )

  val anUserId = "userid-1"
  val unknownUserId = "userid-u"
  val aFormBundleId: String = "012345678901"
  val anEmail = "test@test.com"
}
