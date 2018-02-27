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

package uk.gov.hmrc.fhregistration.models.fhdds

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

case class SentEmailRequest(to: List[String], templateId: String, force: Boolean)

object SentEmailRequest {
  implicit val format = new Format[SentEmailRequest] {
    def reads(json: JsValue): JsResult[SentEmailRequest] = (
      (__ \ "to").read[List[String]] and
        (__ \ "templateId").read[String] and
        (__ \ "force").readNullable[Boolean].map(_.getOrElse(false))) (SentEmailRequest.apply _).reads(json)

    def writes(o: SentEmailRequest): JsValue = Json.writes[SentEmailRequest].writes(o)
  }
}