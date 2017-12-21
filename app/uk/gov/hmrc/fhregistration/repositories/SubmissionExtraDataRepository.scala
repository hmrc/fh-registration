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

import javax.inject.Inject

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SubmissionExtraDataRepository @Inject() (implicit rmc: ReactiveMongoComponent)
  extends ReactiveRepository[SubmissionExtraData, BSONObjectID](
    "submission-extra-data",
    rmc.mongoConnector.db,
    SubmissionExtraData.formats,
    ReactiveMongoFormats.objectIdFormats)

    with AtomicUpdate[SubmissionExtraData]
{

  def saveBusinessRegistrationDetails(userId: String, formTypeRef: String, brd: BusinessRegistrationDetails) = {
    atomicUpsert(
      finder = findSubmissionDataBSON(userId, formTypeRef),
      set(BSONDocument("businessRegistrationDetails" → Json.toJson(brd))))
      .map { result ⇒ result.updateType.savedValue.businessRegistrationDetails}
  }

  def updateFormId(userId: String, formTypeRef: String, formId: String): Future[Boolean] = {
    atomicUpdate(
      finder = findSubmissionDataBSON(userId, formTypeRef),
      set(BSONDocument("formId" → formId)))
      .map(_.isDefined)
  }

  def updateRegistrationNumber(formId: String, submissionRef: String, registrationNumber: String): Future[Boolean] = {
    atomicUpdate(
      finder = findSubmissionDataBSONByFormId(formId),
      set(BSONDocument("submissionRef" → submissionRef, "registrationNumber" → registrationNumber)))
      .map(_.isDefined)
  }

  def updateAuthorization(userId: String, formTypeRef: String, authorization: String): Future[Boolean] = {
    atomicUpdate(
      finder = findSubmissionDataBSON(userId, formTypeRef),
      set(BSONDocument("authorization" → authorization)))
      .map(_.isDefined)
  }

  def findSubmissionExtraDataByFormId(formId: String) = {
    collection.find(findSubmissionDataBSONByFormId(formId)).one[SubmissionExtraData]
  }

  def findSubmissionExtraDataBySubmissionRef(submissionRef: String) = {
    collection.find(findSubmissionDataBSONBySubmissionRef(submissionRef)).one[SubmissionExtraData]
  }

  def findSubmissionExtraData(userId: String, formTypeRef: String) = {
    collection.find(findSubmissionDataBSON(userId, formTypeRef)).one[SubmissionExtraData]
  }

  private def findSubmissionDataBSON(userId: String, formTypeRef: String): BSONDocument = {
    and(BSONDocument("encUserId" -> userId), BSONDocument("formTypeRef"-> formTypeRef))
  }

  private def findSubmissionDataBSONByFormId(formId: String): BSONDocument = {
    BSONDocument("formId" -> formId)
  }

  private def findSubmissionDataBSONBySubmissionRef(submissionRef: String): BSONDocument = {
    BSONDocument("submissionRef" -> submissionRef)
  }

  override def isInsertion(newRecordId: BSONObjectID, oldRecord: SubmissionExtraData): Boolean = oldRecord.id match {
    case null ⇒ false
    case id   ⇒ newRecordId.equals(id)
  }

  override def ensureIndexes(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Seq[scala.Boolean]] = {
    Future.sequence(Seq(
      collection.indexesManager.ensure(
        Index(Seq("userId" -> IndexType.Ascending), name = Some("userIdIdx"), unique = false, sparse = true)),
      collection.indexesManager.ensure(
        Index(Seq("formId" -> IndexType.Ascending), name = Some("formIdIdx"), unique = false, sparse = true)),
      collection.indexesManager.ensure(
        Index(Seq("submissionRef" -> IndexType.Ascending), name = Some("submissionRefIdx"), unique = false, sparse = true))))
  }
}
