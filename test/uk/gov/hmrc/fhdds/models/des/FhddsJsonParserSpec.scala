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

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class FhddsJsonParserSpec extends UnitSpec {

  "FhddsApplication json parser" should {
    "parse valid jsons" in {
      canParse("limited-company-example.json")
      canParse("limited-company-example-minimum.json")
      canParse("valid-test-data.json")
    }
  }

  def canParse(name: String) = {
    val json = Json.parse(getJsonInputStream(name))
    val application = json.as[FhddsApplication]
    application should not be null

    Json.toJson(application) shouldEqual json
  }

  def getJsonInputStream(name: String) = {
    getClass.getResourceAsStream(s"/json/valid/$name")
  }

}
