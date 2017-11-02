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

package uk.gov.hmrc.fhdds.models.dfsStore

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, OFormat}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.CreationAndLastModifiedDetail
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class Submission(
  submissionRef: String,
  submissionMark: Option[String],
  state: String,
  formData: String,
  formTypeRef: String,
  formId: String,
  casKey: Option[String],
  savedDate: Option[DateTime],
  submittedDate: Option[DateTime],
  userId: String,
  expireAt: DateTime,
  crudDetail: CreationAndLastModifiedDetail)

object Submission {

  implicit val BSONObjectIDFormat = ReactiveMongoFormats.objectIdFormats
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  val mongoFormats = ReactiveMongoFormats.mongoEntity({
    implicit val BSONObjectIDFormat: Format[BSONObjectID] = ReactiveMongoFormats.objectIdFormats
    implicit val dateFormat: Format[DateTime] = ReactiveMongoFormats.dateTimeFormats
    Format(Json.reads[Submission], Json.writes[Submission])
  })
  implicit val oFormat: OFormat[Submission] = Json.format[Submission]
}
