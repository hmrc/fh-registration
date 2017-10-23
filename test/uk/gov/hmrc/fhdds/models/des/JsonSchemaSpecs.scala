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


import com.eclipsesource.schema._
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec

class JsonSchemaSpecs extends UnitSpec  {
  val schemaAsJson = Json parse getClass.getResourceAsStream("/schemas/des-schema-alpha-v0.1.json")
  val schema = Json.fromJson[SchemaType](schemaAsJson).get
  val validator = new SchemaValidator().validate(schema) _

  "Json Schema" should {
    "accept valid json" in {
      isValid("valid/valid-test-data.json") shouldBe true
      isValid("valid/limited-company-example.json") shouldBe true
      isValid("valid/limited-company-example-minimum.json") shouldBe true
    }

    "not accept valid json" in {
      isValid("invalid/has-other-storage-sites.json") shouldBe false
      isValid("invalid/intended-trading-start-date.json") shouldBe false
      isValid("invalid/new-fulfilment-business.json") shouldBe false
      isValid("invalid/place-of-business-last-3-years.json") shouldBe false
    }
  }

  def isValid(jsFile: String): Boolean = {
    val json = Json.parse(getJsonInputStream(jsFile))
    val validationResult = validator(json)

    validationResult.isSuccess
  }

  def getJsonInputStream(name: String) = {
    getClass.getResourceAsStream(s"/json/$name")
  }
}
