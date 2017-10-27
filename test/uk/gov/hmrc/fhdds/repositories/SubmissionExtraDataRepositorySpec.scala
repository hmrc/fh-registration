/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.fhdds.repositories

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.fhdds.models.businessregistration.{Address, BusinessRegistrationDetails}
import uk.gov.hmrc.mongo.{Awaiting, CurrentTime, MongoSpecSupport}
import uk.gov.hmrc.play.test.LogCapturing

class SubmissionExtraDataRepositorySpec
  extends WordSpec
    with Matchers
    with MongoSpecSupport
    with BeforeAndAfterEach
    with Awaiting
    with CurrentTime
    with Eventually
    with LogCapturing {

  val repository = new SubmissionExtraDataRepository()
  val anUserId = "userid-1"
  val unknownUserId = "userid-u"
  val formTypeRef = "fhdds-limited-company"
  val aCompanyName = "Some company"
  val anotherCompanyName = "Other company"
  val aFormId = "111-333-444"

  override protected def beforeEach(): Unit = {
    await(repository.removeAll())
  }

  "Saving an object" should {
    "not upsert" in {

      await(repository.drop)
      await(repository.ensureIndexes)

      val originalSave = mkExtraData(aCompanyName)
      await(repository.insert(originalSave))

      val notUpsert = originalSave
        .copy(businessRegistrationDetails = originalSave.businessRegistrationDetails
          .copy("should-not-upsert"))
      a [DatabaseException] should be thrownBy await(repository.insert(notUpsert))
    }
  }

  "Updating a bpr" should {
    "update with empty bpr" in {
      repository.saveBusinessRegistrationDetails(anUserId, formTypeRef, mkBusinessRegistrationDetails(aCompanyName))
    }
  }

  "Updating formId" should {
    "return true" in {
      await(repository.saveBusinessRegistrationDetails(anUserId, formTypeRef, mkBusinessRegistrationDetails(aCompanyName)))
      val result = repository.updateFormId(anUserId, formTypeRef, aFormId)
      await(result) shouldBe true
    }

    "return false" in {
      val result = repository.updateFormId(unknownUserId, formTypeRef, aFormId)
      await(result) shouldBe false
    }

  }

  def mkExtraData(companyName: String) =
    SubmissionExtraData(
      encUserId = "====",
      formTypeRef = "fhdds-limited-company",
      formId = None,
      submissionRef = None,
      businessRegistrationDetails = mkBusinessRegistrationDetails(companyName),
      companyRegistrationNumber = None
    )

  def mkBusinessRegistrationDetails(name: String) = {
    val address = Address(
      "line1",
      "line2",
      None,
      None,
      Some("AA11 1AA"),
      "GB"
    )
    BusinessRegistrationDetails(
      name,
      Some("Organization"),
      address,
      "12345",
      "12313212",
      isAGroup = false,
      directMatch = false,
      agentReferenceNumber = None,
      firstName = None,
      lastName = None,
      utr = None,
      identification = None,
      false
    )
  }

}
