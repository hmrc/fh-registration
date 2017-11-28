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
import uk.gov.hmrc.fhdds.services.{FhddsApplicationService, FhddsApplicationServiceImpl}
import uk.gov.hmrc.play.test.UnitSpec
import com.eclipsesource.schema._

import scala.xml.XML

class subscriptionCreateRequestSchemaServiceSpec extends UnitSpec {

  val schemaAsJson = Json parse getClass.getResourceAsStream("/schemas/des-schema-alpha-v0.1.json")
  val schema = Json.fromJson[SchemaType](schemaAsJson).get
  val validator = new SchemaValidator().validate(schema) _
  val service = new FhddsApplicationServiceImpl

  val brd: BusinessRegistrationDetails = Json
    .parse(getClass.getResourceAsStream("/models/business-registration-details-sole-trader.json"))
    .as[BusinessRegistrationDetails]

  "Application service" should {
    "Create a correct json for fhdds-limited-company-large-uk.xml" in {
      validatesFor("fhdds-limited-company-large-uk.xml")
    }

//    "Create a correct json for fhdds-limited-company-large-uk-without-addressLine2.xml" in {
//      validatesFor("fhdds-limited-company-large-uk-without-addressLine2.xml")
//    }

    "Create a correct json for fhdds-limited-company-minimum.xml" in {
      validatesFor("fhdds-limited-company-minimum.xml")
    }

//    "Create a correct json for fhdds-limited-company-minimum-international.xml" in {
//      validatesFor("fhdds-limited-company-minimum-international.xml")
//    }
  }

  def validatesFor(file: String) = {
    val iform = loadSubmission(file)
    val subscrtiptionCreate = service.iformXmlToApplication(iform, brd)

    val json = Json.toJson(subscrtiptionCreate)

    val validationResult = validator(json)
    validationResult.isSuccess shouldEqual true
  }


  def loadSubmission(file: String): generated.Data = {
    val xml = XML
      .load(getClass.getResourceAsStream(s"/xml/valid/$file"))
    scalaxb.fromXML[generated.Data](xml)
  }

}
