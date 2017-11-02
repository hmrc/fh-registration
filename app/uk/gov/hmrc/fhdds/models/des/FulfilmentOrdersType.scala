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

package uk.gov.hmrc.fhdds.models.des

import play.api.libs.json._

sealed trait FulfilmentOrdersType

case class OnLineOnly(onlineOnly: Boolean = true,
                      onlineAndTelephone: Boolean = false,
                      onlineTelephoneAndPhysical: Boolean = false,
                      all: Boolean = false,
                      other: Boolean = false,
                      typeOfOtherOrder: String = "NA") extends FulfilmentOrdersType

object OnLineOnly {
  implicit val format = Json.format[OnLineOnly]
}

case class NotOnLineOnly(onLine: Boolean = true,
                         telephone: Boolean = true,
                         physicalPremises: Boolean = true,
                         other: Boolean = false,
                         typeOfOtherOrder: Option[String] = None) extends FulfilmentOrdersType

object NotOnLineOnly {
  implicit val format = Json.format[NotOnLineOnly]
}

object FulfilmentOrdersType {

  val reads: Reads[FulfilmentOrdersType] = new Reads[FulfilmentOrdersType] {
    override def reads(json: JsValue): JsResult[FulfilmentOrdersType] = json.validate[JsObject].flatMap{ o ⇒
      if (o.keys.contains("onlineOnly")) json.validate[OnLineOnly]
      else json.validate[NotOnLineOnly]
    }
  }

  val writes: Writes[FulfilmentOrdersType] = new Writes[FulfilmentOrdersType]{
    override def writes(o: FulfilmentOrdersType) = o match {
      case notOnLineOnly: NotOnLineOnly ⇒ NotOnLineOnly.format.writes(notOnLineOnly)
      case onLineOnly: OnLineOnly ⇒ OnLineOnly.format.writes(onLineOnly)
    }
  }
  implicit  val format: Format[FulfilmentOrdersType] = Format(reads, writes)
}