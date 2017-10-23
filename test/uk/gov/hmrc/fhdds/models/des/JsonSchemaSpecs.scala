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
    validate("valid-test-data")
    validate("limited-company-example")
    validate("limited-company-example-minimum")

    validate("fhdds-limited-company-minimum-international")
    validate("fhdds-limited-company-minimum")
    validate("fhdds-limited-company-large-uk")

    invalidate("has-other-storage-sites") 
    invalidate("intended-trading-start-date") 
    invalidate("new-fulfilment-business") 
    invalidate("place-of-business-last-3-years") 
  }

  def validate(name: String) = {
    s"accept valid json $name" in {
      isValid(s"valid/$name.json") shouldBe true
    }
  }
  def invalidate(name: String) = {
    s"reject invalid json $name" in {
      isValid(s"invalid/$name.json") shouldBe false
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
