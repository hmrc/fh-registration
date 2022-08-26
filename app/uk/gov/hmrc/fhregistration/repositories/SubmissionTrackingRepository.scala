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

import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes, ReturnDocument, Updates}
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress.EnrolmentProgress
import uk.gov.hmrc.fhregistration.repositories.SubmissionTracking._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultSubmissionTrackingRepository])
trait SubmissionTrackingRepository {

  def findAll(): Future[List[SubmissionTracking]]

  def findSubmissionTrackingByUserId(userId: String): Future[Option[SubmissionTracking]]

  def findSubmissionTrackingByFormBundleId(formBundleId: String): Future[Option[SubmissionTracking]]

  def deleteSubmissionTackingByFormBundleId(formBundleId: String): Future[Int]
  def deleteSubmissionTackingByRegistrationNumber(userId: String, registrationNumber: String): Future[Int]

  def insertSubmissionTracking(submissionTracking: SubmissionTracking): Future[_]

  def updateEnrolmentProgress(formBundleId: String, progress: EnrolmentProgress): Future[Seq[SubmissionTracking]]
}

class DefaultSubmissionTrackingRepository @Inject()(implicit mongo: MongoComponent, implicit val ec: ExecutionContext)
    extends PlayMongoRepository[SubmissionTracking](
      collectionName = "submission-tracking",
      mongoComponent = mongo,
      domainFormat = SubmissionTracking.formats,
      indexes = Seq(
        IndexModel(
          Indexes.ascending(UserIdField),
          IndexOptions()
            .name("userIdIdx")
            .sparse(true)
            .unique(false)
        ),
        IndexModel(
          Indexes.ascending(FormBundleIdField),
          IndexOptions()
            .name("formBundleIdIdx")
            .unique(false)
            .sparse(true)
        ),
        IndexModel(
          Indexes.ascending(RegistrationNumberField),
          IndexOptions()
            .name("registrationNumberIdx")
            .unique(false)
            .sparse(true)
        )
      )
    ) with SubmissionTrackingRepository {

  import SubmissionTracking._

  override def findSubmissionTrackingByUserId(userId: String): Future[Option[SubmissionTracking]] =
    collection
      .find(Filters.equal(UserIdField, userId))
      .headOption()

  override def findSubmissionTrackingByFormBundleId(formBundleId: String): Future[Option[SubmissionTracking]] =
    collection
      .find(Filters.equal(FormBundleIdField, formBundleId))
      .headOption()

  override def deleteSubmissionTackingByFormBundleId(formBundleId: String): Future[Int] =
    collection
      .deleteOne(Filters.equal(FormBundleIdField, formBundleId))
      .toFuture()
      .map(_.getDeletedCount.toInt)

  def deleteSubmissionTackingByRegistrationNumber(userId: String, registrationNumber: String): Future[Int] =
    collection
      .deleteOne(
        Filters.and(
          Filters.equal(UserIdField, userId),
          Filters.equal(RegistrationNumberField, registrationNumber)
        ))
      .toFuture()
      .map(_.getDeletedCount.toInt)

  override def insertSubmissionTracking(submissionTracking: SubmissionTracking): Future[_] =
    collection
      .findOneAndUpdate(
        filter = Filters.equal(UserIdField, submissionTracking.userId),
        update = Updates.combine(
          Updates.set("userId", submissionTracking.userId),
          Updates.set("formBundleId", submissionTracking.formBundleId),
          Updates.set("email", submissionTracking.email),
          Updates.set("submissionTime", submissionTracking.submissionTime),
          Updates.set("enrolmentProgressOpt", Codecs.toBson(submissionTracking.enrolmentProgressOpt)),
          Updates.set("registrationNumber", Codecs.toBson(submissionTracking.registrationNumber))
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .collect()
      .toFuture()

  override def updateEnrolmentProgress(
    formBundleId: String,
    progress: EnrolmentProgress): Future[Seq[SubmissionTracking]] =
    collection
      .findOneAndUpdate(
        filter = Filters.equal(FormBundleIdField, formBundleId),
        update = Updates.set(EnrolmentProgressField, progress.toString)
      )
      .collect()
      .toFuture()

  override def findAll(): Future[List[SubmissionTracking]] = collection.find(Filters.empty()).toFuture().map(_.toList)
}
