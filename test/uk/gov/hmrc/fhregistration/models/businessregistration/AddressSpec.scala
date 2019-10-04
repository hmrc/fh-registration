/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.models.businessregistration

import uk.gov.hmrc.fhregistration.util.UnitSpec

class AddressSpec extends UnitSpec{
  "Address toString" should {
    "Format correctly" in {
      val address = Address("testLine1", "testLine2", Some("testLine3"), Some("testLine4"), Some("testPostcode"), "testCountry")

      address.toString shouldBe "testLine1, testLine2, testLine3, testLine4, testPostcode, testCountry"
    }
  }
}
