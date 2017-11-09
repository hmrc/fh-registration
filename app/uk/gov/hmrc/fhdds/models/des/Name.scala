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

sealed trait NameType

case class Name(firstName: String = "firstName",
                middleName: Option[String] = None,
                lastName: String = "lastName") extends NameType

object Name {
  implicit val format = Json.format[Name]
}

case class Names(companyName: Option[String] = None, tradingName: Option[String] = None) extends NameType

object Names{
  implicit val format = Json.format[Names]
}


object NameType {

  val reads: Reads[NameType] = new Reads[NameType] {
    override def reads(json: JsValue): JsResult[NameType] = json.validate[JsObject].flatMap{ o ⇒
      if (o.keys.contains("firstName")) json.validate[Name]
      else json.validate[Names]
    }
  }

  val writes: Writes[NameType] = new Writes[NameType]{
    override def writes(o: NameType) = o match {
      case name: Name ⇒ Name.format.writes(name)
      case names: Names ⇒ Names.format.writes(names)
    }
  }
  implicit  val format: Format[NameType] = Format(reads, writes)
}