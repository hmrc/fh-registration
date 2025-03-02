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

package uk.gov.hmrc.fhregistration.models.fhdds

import play.api.libs.json._

object EnrolmentProgress extends Enumeration {

  type EnrolmentProgress = Value
  val Pending, Unknown, Error = Value

  implicit val format: Format[EnrolmentProgress.Value] = new Format[EnrolmentProgress.Value] {

    def reads(json: JsValue): JsResult[EnrolmentProgress.Value] = json match {
      case JsString(value) =>
        EnrolmentProgress.values.find(_.toString == value) match {
          case Some(progress) => JsSuccess(progress)
          case None           => JsError("Invalid EnrolmentProgress value")
        }
      case _ => JsError("Expected a string value")
    }

    def writes(progress: EnrolmentProgress.Value): JsValue = JsString(progress.toString)
  }
}
