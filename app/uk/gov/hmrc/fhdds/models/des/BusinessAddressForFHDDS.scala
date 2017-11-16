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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json.{JsString, Json, Reads, Writes}

case class PreviousOperationalAddress(previousAddress: Address,
                                      operatingDate: LocalDate)

object PreviousOperationalAddress {
  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  implicit val localDateReads = Reads.localDateReads("yyyy-MM-dd")
  implicit val localDateWrites = Writes { date: LocalDate ⇒
    JsString(date.format(dateTimeFormatter))
  }

  implicit val format = Json.format[PreviousOperationalAddress]

}

case class BusinessAddressForFHDDS(currentAddress: Address,
                                   commonDetails: CommonDetails,
                                   dateStartedTradingAsFulfilmentHouse: LocalDate,
                                   isOnlyPrinicipalPlaceOfBusinessInLastThreeYears: Boolean,
                                   previousOperationalAddress: Option[List[PreviousOperationalAddress]] = None)

object BusinessAddressForFHDDS {
  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  implicit val localDateReads = Reads.localDateReads("yyyy-MM-dd")
  implicit val localDateWrites = Writes { date: LocalDate ⇒
    JsString(date.format(dateTimeFormatter))
  }

  implicit val format = Json.format[BusinessAddressForFHDDS]

}