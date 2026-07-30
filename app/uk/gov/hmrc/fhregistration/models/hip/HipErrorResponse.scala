/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OFormat, Reads, __}

case class HipFailure(`type`: String, reason: String)

object HipFailure {
  implicit val format: OFormat[HipFailure] = Json.format[HipFailure]
}

case class HipErrorResponse(code: String, reason: String, logId: Option[String] = None)

object HipErrorResponse {

  private val systemErrorReads: Reads[HipErrorResponse] =
    ((__ \ "response" \ "error" \ "code").read[String] and
      (__ \ "response" \ "error" \ "message").read[String] and
      (__ \ "response" \ "error" \ "logID").readNullable[String])(HipErrorResponse.apply)

  private val failuresReads: Reads[HipErrorResponse] =
    (__ \ "response").read[Seq[HipFailure]].map { failures =>
      HipErrorResponse(
        code = failures.map(_.`type`).mkString(","),
        reason = failures.map(_.reason).mkString("; ")
      )
    }

  private val nestedFailuresReads: Reads[HipErrorResponse] =
    (__ \ "response" \ "failures").read[Seq[HipFailure]].map { failures =>
      HipErrorResponse(
        code = failures.map(_.`type`).mkString(","),
        reason = failures.map(_.reason).mkString("; ")
      )
    }

  implicit val reads: Reads[HipErrorResponse] =
    systemErrorReads.orElse(failuresReads).orElse(nestedFailuresReads)
}
