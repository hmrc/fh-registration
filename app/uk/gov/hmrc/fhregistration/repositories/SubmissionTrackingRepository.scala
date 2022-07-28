/*
 * Copyright 2022 HM Revenue & Customs
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
import com.google.inject.ImplementedBy
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{Collation, UpdateWriteResult}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers.BSONDocumentWrites
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress.EnrolmentProgress
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultSubmissionTrackingRepository])
trait SubmissionTrackingRepository {

  def findSubmissionTrackingByUserId(userId: String): Future[Option[SubmissionTracking]]

  def findSubmissionTrakingByFormBundleId(formBundleId: String): Future[Option[SubmissionTracking]]

  def deleteSubmissionTackingByFormBundleId(formBundleId: String): Future[Int]
  def deleteSubmissionTackingByRegistrationNumber(userId: String, registrationNumber: String): Future[Int]

  def insertSubmissionTracking(submissionTracking: SubmissionTracking): Future[_]

  def updateEnrolmentProgress(formBundleId: String, progress: EnrolmentProgress): Future[UpdateWriteResult]
}

class DefaultSubmissionTrackingRepository @Inject()(
  implicit rmc: ReactiveMongoComponent,
  implicit val ec: ExecutionContext)
    extends ReactiveRepository[SubmissionTracking, BSONObjectID](
      "submission-tracking",
      rmc.mongoConnector.db,
      SubmissionTracking.formats,
      ReactiveMongoFormats.objectIdFormats) with SubmissionTrackingRepository {

  import SubmissionTracking.{EnrolmentProgressField, FormBundleIdField, RegistrationNumberField, UserIdField}

  override def findSubmissionTrackingByUserId(userId: String) =
    collection.find(BSONDocument(UserIdField → userId), None).one[SubmissionTracking]

  override def findSubmissionTrakingByFormBundleId(formBundleId: String) =
    collection.find(BSONDocument(FormBundleIdField → formBundleId), None).one[SubmissionTracking]

  override def deleteSubmissionTackingByFormBundleId(formBundleId: String): Future[Int] =
    collection.delete
      .one(BSONDocument(FormBundleIdField → formBundleId))
      .map(_.n)

  def deleteSubmissionTackingByRegistrationNumber(userId: String, registrationNumber: String): Future[Int] =
    collection.delete
      .one(BSONDocument(UserIdField → userId, RegistrationNumberField → registrationNumber))
      .map(_.n)

  override def insertSubmissionTracking(submissionTracking: SubmissionTracking): Future[_] =
    collection.findAndUpdate(
      BSONDocument(UserIdField → submissionTracking.userId),
      submissionTracking,
      fetchNewObject = false,
      upsert = true,
      sort = None,
      fields = None,
      bypassDocumentValidation = false,
      writeConcern = WriteConcern.Default,
      maxTime = Option.empty[FiniteDuration],
      collation = Option.empty[Collation],
      arrayFilters = Seq.empty
    )

  override def updateEnrolmentProgress(formBundleId: String, progress: EnrolmentProgress): Future[UpdateWriteResult] =
    collection
      .update(ordered = false)
      .one(
        BSONDocument(FormBundleIdField → formBundleId),
        BSONDocument("$set" → BSONDocument(EnrolmentProgressField → progress.toString))
      )

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    val indexes = Future sequence Seq(
      collection.indexesManager.ensure(
        Index(Seq(UserIdField -> IndexType.Ascending), name = Some("userIdIdx"), unique = false, sparse = true)),
      collection.indexesManager.ensure(
        Index(
          Seq(FormBundleIdField -> IndexType.Ascending),
          name = Some("formBundleIdIdx"),
          unique = false,
          sparse = true)),
      collection.indexesManager.ensure(
        Index(
          Seq(FormBundleIdField -> IndexType.Ascending),
          name = Some("registrationNumberIdx"),
          unique = false,
          sparse = true))
    )

    for {
      s ← super.ensureIndexes
      i ← indexes
    } yield {
      s ++ i
    }
  }

}
