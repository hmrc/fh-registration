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

import com.eclipsesource.schema.{SchemaType, SchemaValidator, _}
import play.api.libs.json.Json
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate.format
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class FhddsApplicationServiceSpec extends UnitSpec {

  val schemaAsJson = Json parse getClass.getResourceAsStream("/schemas/des-schema-r1.json")
  val schema = Json.fromJson[SchemaType](schemaAsJson).get
  val validator = new SchemaValidator().validate(schema) _
  val service = new FhddsApplicationServiceImpl(new CountryCodeLookupImpl)

  val brd: BusinessRegistrationDetails = Json
    .parse(getClass.getResourceAsStream("/models/business-registration-details-sole-trader.json"))
    .as[BusinessRegistrationDetails]

  "Application service" should {
    "Create a correct json for fhdds-limited-company-large-uk.xml" in {
      validatesFor("fhdds-limited-company-large-uk.xml")
    }


    "Create a correct json for fhdds-limited-company-minimum.xml" in {
      validatesFor("fhdds-limited-company-minimum.xml")
    }

    "Create a correct json for fhdds-limited-company-minimum-international.xml" in {
      val request = validatesFor("fhdds-limited-company-minimum-international.xml")

      request.subScriptionCreate.contactDetail.address.map(_.countryCode) shouldEqual Some("BG")
      request.subScriptionCreate.contactDetail.address.flatMap(_.line4) shouldEqual Some("Bulgaria")
    }

    "Create a correct json for fhdds-limited-company-minimum-with-ggemail.xml" in {
      val request = validatesFor("fhdds-limited-company-minimum-with-ggemail.xml")

      request.subScriptionCreate.declaration.email shouldEqual Some("cosmin@cosmin.co.uk")
    }

  }

  def validatesFor(file: String): SubScriptionCreate = {
    val iform = loadSubmission(file)
    val subscrtiptionCreate = service.iformXmlToApplication(iform, brd)


    val json = Json.toJson(subscrtiptionCreate)

    val validationResult = validator(json)
    validationResult.fold(
      invalid = {errors ⇒ println(errors.toJson)},
      valid = {v ⇒ println("OK")}
    )
    validationResult.isSuccess shouldEqual true
    subscrtiptionCreate
  }


  def loadSubmission(file: String): generated.Data = {
    val xml = XML
      .load(getClass.getResourceAsStream(s"/xml/valid/$file"))
    scalaxb.fromXML[generated.Data](xml)
  }

}
