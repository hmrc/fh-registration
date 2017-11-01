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

import com.eclipsesource.schema.{SchemaType, SchemaValidator}
import play.api.libs.json.Json
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhdds.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhdds.services.FhddsApplicationService
import uk.gov.hmrc.play.test.UnitSpec
import com.eclipsesource.schema._

import scala.xml.XML

class FhddsApplicationServiceSpec extends UnitSpec {

  val schemaAsJson = Json parse getClass.getResourceAsStream("/schemas/des-schema-alpha-v0.1.json")
  val schema = Json.fromJson[SchemaType](schemaAsJson).get
  val validator = new SchemaValidator().validate(schema) _
  val service = new FhddsApplicationService {}

  val brd = Json
    .parse(getClass.getResourceAsStream("/models/business-registration-details.json"))
    .as[BusinessRegistrationDetails]

  "Application service" should {
    "Create a correct json" in {
      val iform = loadSubmission("fhdds-limited-company-large-uk.xml")
      val subscrtiptionCreate = service.iformXmlToApplication(iform, brd)

      val json = Json.toJson(subscrtiptionCreate)

      println(s"==== ${json.toString()}");
      val validationResult = validator(json)
      validationResult.fold (
        invalid = { errors =>  println(errors.toJson) },
        valid = { post => println("ok") }
      )
      validationResult.isSuccess shouldEqual true

    }
  }


  def loadSubmission(file: String): generated.Data = {
    val xml = XML
      .load(getClass.getResourceAsStream(s"/xml/valid/$file"))
    scalaxb.fromXML[generated.Data](xml)
  }

}
