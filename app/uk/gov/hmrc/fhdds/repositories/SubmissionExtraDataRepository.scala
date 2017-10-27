package uk.gov.hmrc.fhdds.repositories

import play.api.libs.json.Json
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, MongoConnector, ReactiveRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionExtraDataRepository(implicit mc: MongoConnector)
  extends ReactiveRepository[SubmissionExtraData, BSONObjectID](
    "submission-extra-data",
    mc.db,
    SubmissionExtraData.formats,
    ReactiveMongoFormats.objectIdFormats)

    with AtomicUpdate[SubmissionExtraData]
{

  def saveBusinessRegistrationDetails(userId: String, formTypeRef: String, brd: BusinessRegistrationDetails) = {
    atomicUpsert(
      finder = findSubmissionDataBSON(userId, formTypeRef),
      set(BSONDocument("businessRegistrationDetails" → Json.toJson(brd))))
      .map { result ⇒
        result.writeResult.
      }
  }

  def updateFormId(userId: String, formTypeRef: String, formId: String): Future[Boolean] = {
    atomicUpdate(
      finder = findSubmissionDataBSON(userId, formTypeRef),
      set(BSONDocument("formId" → formId)))
      .map(_.isDefined)
  }

  def findSubmissionDataBSON(userId: String, formTypeRef: String): BSONDocument = {
    and(BSONDocument("encUserId" -> userId), BSONDocument("formTypeRef"-> formTypeRef))
  }

  def findSubmissionDataBSON(submissionRef: String): BSONDocument = {
    BSONDocument("submissionRef" -> submissionRef)
  }

  def findBusinessRegistrationDetails(submissionRef: String) = {
    collection.find(findSubmissionDataBSON(submissionRef)).one[SubmissionExtraData]
  }

  def findBusinessRegistrationDetails(userId: String, formTypeRef: String) = {
    collection.find(findSubmissionDataBSON(userId, formTypeRef)).one[SubmissionExtraData]
  }

  override def isInsertion(newRecordId: BSONObjectID, oldRecord: SubmissionExtraData): Boolean = oldRecord.id match {
    case id => newRecordId.equals(id)
    case _ => false
  }

}
