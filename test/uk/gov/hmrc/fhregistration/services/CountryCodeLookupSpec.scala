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

import uk.gov.hmrc.play.test.UnitSpec

class CountryCodeLookupSpec extends UnitSpec {

  val countryCodeLookup = new CountryCodeLookupImpl

  "Country code lookup" should {
    "Return AD" in {
      countryCodeLookup.countryCode("Andorra") shouldEqual Some("AD")
    }

    "Return AD for upper case input" in {
      countryCodeLookup.countryCode("ANDORRA") shouldEqual Some("AD")
    }

    "Return None for  Saint Barthélemy" in {
      countryCodeLookup.countryCode("Saint Barthélemy") shouldEqual None
    }

    "Return DO" in {
      countryCodeLookup.countryCode("Dominican Republic (the)") shouldEqual Some("DO")
    }

    "Return ZW" in {
      countryCodeLookup.countryCode("Zimbabwe") shouldEqual Some("ZW")
    }
  }

}
