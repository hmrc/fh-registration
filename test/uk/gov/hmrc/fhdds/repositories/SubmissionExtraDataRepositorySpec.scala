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
