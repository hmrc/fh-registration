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

package uk.gov.hmrc.fhdds.models.iform

import java.io.InputStream

import play.api.libs.json.Json
import uk.gov.hmrc.fhdds.models.des.FhddsApplication
import uk.gov.hmrc.play.test.UnitSpec

class IformApplicationSpec extends UnitSpec {

  "Parse Json" should {
    "read a companies house result" in {
      val stream: InputStream = getClass.getResourceAsStream("/models/IformMin.json")
      val result = Json.parse(stream).as[FhddsApplication]
      result.organizationType shouldBe "Limited Liability Partnership"
    }

  }

}
