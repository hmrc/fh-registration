package uk.gov.hmrc.fhdds.repositories

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails

case class SubmissionExtraData(
  //id
  encUserId: String,
  formTypeRef: String,
  formId: Option[String],
  submissionRef: Option[String],
  businessRegistrationDetails: BusinessRegistrationDetails,
  companyRegistrationNumber: Option[String],
  id: BSONObjectID = BSONObjectID.generate
)

object SubmissionExtraData {
  import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.{objectIdFormats, mongoEntity}

  val formats = mongoEntity (Json.format[SubmissionExtraData])
}
