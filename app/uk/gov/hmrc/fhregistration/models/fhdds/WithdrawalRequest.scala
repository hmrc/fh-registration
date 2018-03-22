package uk.gov.hmrc.fhregistration.models.fhdds

import play.api.libs.json.{JsValue, Json}

case class WithdrawalRequest(
  emailAddress: String,
  withdrawal: JsValue
)

object WithdrawalRequest {
  implicit val withdrawalRequestFormat = Json.format[WithdrawalRequest]
}