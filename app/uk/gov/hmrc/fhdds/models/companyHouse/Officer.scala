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

package uk.gov.hmrc.fhdds.models.companyHouse

import com.github.tototoshi.play.json.JsonNaming
import org.joda.time.LocalDate
import play.api.libs.json.Json

case class Officer(
  name       : String,
  resignedOn : Option[LocalDate],
  officerRole: String
) {

  def lastName = name.split(",")(0).trim

  def firstName = {
    val parts = name.split(",")
    if (parts.length > 1) parts(1).trim
    else ""
  }
}


object Officer {
  implicit val officerResultReader = JsonNaming snakecase Json.reads[Officer]
}
