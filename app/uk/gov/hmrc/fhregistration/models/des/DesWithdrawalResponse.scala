package uk.gov.hmrc.fhregistration.models.des

import java.util.Date

import play.api.libs.json.Json

case class DesWithdrawalResponse (processingDate: Date)

object DesWithdrawalResponse {
  implicit val submissionResponseFormat = Json.format[DesWithdrawalResponse]
}