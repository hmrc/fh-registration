/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.models.hip

import play.api.libs.json.*
import uk.gov.hmrc.fhregistration.models.des.DesStatus

object HipStatus extends Enumeration {
  type HipStatus = Value

  val NoFormBundleForm = Value("No Form Bundle Found")
  val RegFormReceived = Value("Reg Form Received")
  val SentToDs = Value("Sent To DS")
  val DsOutcomeInProgress = Value("DS Outcome In Progress")
  val Successful = Value("Successful")
  val Rejected = Value("Rejected")
  val InProcessing = Value("In processing")
  val Withdrawal = Value("Withdrawal")
  val SentToRcm = Value("Sent to RCM")
  val ApprovedWithConditions = Value("Approved with Conditions")
  val Revoked = Value("Revoked")
  val Deregistered = Value("De-Registered")
  val ContractObjectInactive = Value("Contract Object Inactive")

  implicit val format: Format[HipStatus.Value] = new Format[HipStatus.Value] {

    def reads(json: JsValue): JsResult[HipStatus.Value] = json match {
      case JsString(value) =>
        HipStatus.values.find(_.toString == value) match {
          case Some(status) => JsSuccess(status)
          case None         => JsError("Invalid HipSatus value")
        }
      case _ => JsError("Expected a string value")
    }

    def writes(status: HipStatus.Value): JsValue = JsString(status.toString)
  }

  def toDesStatus(hipStatus: HipStatus): DesStatus.Value =
    DesStatus.values.find(_.toString == hipStatus.toString) match {
      case Some(status) => status
      case None         => throw RuntimeException("Invalid HipStatus value")
    }
}
