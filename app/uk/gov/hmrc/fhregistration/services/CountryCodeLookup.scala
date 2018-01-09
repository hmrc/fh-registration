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

package uk.gov.hmrc.fhregistration.services

import javax.inject.Singleton

import com.google.inject.ImplementedBy
import play.api.libs.json.{JsObject, Json}

@Singleton
class CountryCodeLookupImpl extends CountryCodeLookup {
  val isoCodes = Json
    .parse(getClass.getResourceAsStream("/iso-country-to-2-letter-code-less-BL.json"))
    .as[JsObject]
    .value
    .map { case (country, code) ⇒ country.toLowerCase -> code.as[String]}

  override def countryCode(country: String) = isoCodes.get(country.toLowerCase)
}

@ImplementedBy(classOf[CountryCodeLookupImpl])
trait CountryCodeLookup {

  def countryCode(country: String): Option[String]

}
