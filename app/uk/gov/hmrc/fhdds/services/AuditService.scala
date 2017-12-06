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

package uk.gov.hmrc.fhdds.services

import play.api.libs.json.{JsObject, JsString, Json}
import uk.gov.hmrc.fhdds.models.des.{DesSubmissionResponse, SubScriptionCreate}
import uk.gov.hmrc.fhdds.models.fhdds.SubmissionRequest
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraData
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import javax.inject.Singleton

import com.google.inject.ImplementedBy

@Singleton
class AuditServiceImpl extends AuditService {

}

@ImplementedBy(classOf[AuditServiceImpl])
trait AuditService {

  val AuditSource = "fhdds"
  val AuditType = "fulfilmentHouseRegistrationSubmission"

  def buildSubmissionAuditEvent(
    submissionRequest: SubmissionRequest,
    application: SubScriptionCreate,
    extraData: SubmissionExtraData,
    desResponse: DesSubmissionResponse,
    submissionRef: String
  )(implicit hc: HeaderCarrier): ExtendedDataEvent = {


    val details = JsObject(Seq(
      "authorization" → Json.toJson(extraData.authorization),
      "submissionRef" → JsString(submissionRef),
      "submissionData" → Json.toJson(application),
      "businessPartnerRecord" → Json.toJson(extraData.businessRegistrationDetails)
    ))

    val customTags = Map(
      "clientIP" -> hc.trueClientIp,
      "clientPort" -> hc.trueClientPort,
      "transactionName" -> Some(s"FHDDS - $submissionRef")
    ) collect {
      case (key, Some(value)) ⇒ key -> value
    }

    ExtendedDataEvent(
      auditSource = AuditSource,
      auditType = AuditType,
      tags = hc.headers.toMap ++ customTags,
      detail = details
    )



  }

}
