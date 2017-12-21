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

package uk.gov.hmrc.fhregistration.repositories

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.fhregistration.models.businessregistration.{Address, BusinessRegistrationDetails}
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

  implicit val reactiveMongoComponent = new ReactiveMongoComponent {
    override def mongoConnector = mongoConnectorForTest
  }

  val repository = new SubmissionExtraDataRepository()
  val anUserId = "userid-1"
  val unknownUserId = "userid-u"
  val formTypeRef = "fhdds-limited-company"
  val aCompanyName = "Some company"
  val anotherCompanyName = "Other company"
  val aFormId = "111-333-444"
  val aSubmissionRef = "YYY-ZZZ1-TTTT"
  val aRegistrationNumber = "XDFH00000100054"

  override protected def beforeEach(): Unit = {
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Updating a bpr" should {
    "write the value to mongo" in {
      val brd = mkBusinessRegistrationDetails(aCompanyName)
      repository.saveBusinessRegistrationDetails(anUserId, formTypeRef, brd)
      val updated = await(repository.findSubmissionExtraData(anUserId, formTypeRef))
      updated.map(_.businessRegistrationDetails) shouldEqual Some(brd)
    }
  }

  "Updating formId" should {
    "save to mongo" in {
      await(repository.saveBusinessRegistrationDetails(anUserId, formTypeRef, mkBusinessRegistrationDetails(aCompanyName)))
      val result = repository.updateFormId(anUserId, formTypeRef, aFormId)
      await(result) shouldBe true
      val updated = await(repository.findSubmissionExtraData(anUserId, formTypeRef))
      updated.flatMap(_.formId) shouldEqual Some(aFormId)
    }

    "return false" in {
      val result = repository.updateFormId(unknownUserId, formTypeRef, aFormId)
      await(result) shouldBe false
    }
  }

  "Updating submissionRef and registration number" should {
    "save to mongo" in {
      await(repository.saveBusinessRegistrationDetails(anUserId, formTypeRef, mkBusinessRegistrationDetails(aCompanyName)))

      val result = repository.updateFormId(anUserId, formTypeRef, aFormId)
      await(result) shouldBe true

      val initially = await(repository.findSubmissionExtraData(anUserId, formTypeRef))
      initially should not be None
      initially.flatMap(_.submissionRef) should be(None)
      initially.flatMap(_.registrationNumber) should be(None)

      val result2 = repository.updateRegistrationNumber(aFormId, aSubmissionRef, aRegistrationNumber)
      await(result2) shouldBe true
      val updated = await(repository.findSubmissionExtraDataBySubmissionRef(aSubmissionRef))

      updated.flatMap(_.submissionRef) shouldEqual  Some(aSubmissionRef)
      updated.flatMap(_.registrationNumber) shouldEqual Some(aRegistrationNumber)
    }
  }

  def mkExtraData(companyName: String) =
    SubmissionExtraData(
      encUserId = "====",
      formTypeRef = "fhdds-limited-company",
      authorization = Some("Bearer som-bearer"),
      formId = None,
      submissionRef = None,
      registrationNumber = None,
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
      isBusinessDetailsEditable = false
    )
  }

}
