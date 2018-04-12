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

package uk.gov.hmrc.fhregistration.repositories

import javax.inject.Inject

import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SubmissionTrackingRepository @Inject() (implicit rmc: ReactiveMongoComponent)
  extends ReactiveRepository[SubmissionTracking, BSONObjectID](
    "submission-tracking",
    rmc.mongoConnector.db,
    SubmissionTracking.formats,
    ReactiveMongoFormats.objectIdFormats)
{


  import SubmissionTracking.{FormBundleIdField, UserIdField}

  def findSubmissionTrackingByUserId(userId: String) = {
    collection.find(BSONDocument(UserIdField → userId)).one[SubmissionTracking]
  }

  def findSubmissionTrakingByFormBundleId(formBundleId: String) = {
    collection.find(BSONDocument(FormBundleIdField → formBundleId)).one[SubmissionTracking]
  }

  def deleteSubmissionTackingByFormBundleId(formBundleId: String): Future[Int] = {
    collection
      .remove(BSONDocument(FormBundleIdField → formBundleId))
      .map( _.n)
  }

  def insertSubmissionTracking(submissionTracking: SubmissionTracking) = {
    collection.insert(submissionTracking)
  }



  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    val indexes = Future sequence Seq(
      collection.indexesManager.ensure(
        Index(Seq(UserIdField -> IndexType.Ascending), name = Some("userIdIdx"), unique = false, sparse = true)),
      collection.indexesManager.ensure(
        Index(Seq(FormBundleIdField -> IndexType.Ascending), name = Some("formBundleIdIdx"), unique = false, sparse = true))
    )

    for {
      s ← super.ensureIndexes
      i ← indexes
    } yield {
      s ++ i
    }
  }

}
