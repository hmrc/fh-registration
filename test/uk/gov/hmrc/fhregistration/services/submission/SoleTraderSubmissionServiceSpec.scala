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

package uk.gov.hmrc.fhregistration.services.submission

import com.eclipsesource.schema.{SchemaType, SchemaValidator, _}
import generated.limited.SoleDataFormat
import org.apache.commons.io.FilenameUtils
import play.api.libs.json.Json
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhregistration.services.CountryCodeLookupImpl
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class SoleTraderSubmissionServiceSpec extends UnitSpec {

  val schemaAsJson = Json parse getClass.getResourceAsStream("/schemas/des-schema-r1.json")
  val schema = Json.fromJson[SchemaType](schemaAsJson).get
  val validator = new SchemaValidator().validate(schema) _
  val service = new SoleTraderSubmissionService(new CountryCodeLookupImpl)

  val brd: BusinessRegistrationDetails = Json
    .parse(getClass.getResourceAsStream("/models/business-registration-details-sole-trader.json"))
    .as[BusinessRegistrationDetails]

  "Application service" should {
    "Create a correct json for sole-proprietor-large-uk.xml" in {
      val request = validatesFor("sole-proprietor-large-uk.xml")


      request.subScriptionCreate.organizationType shouldBe "Sole Proprietor"
      request.subScriptionCreate.businessDetail.nonProprietor shouldBe None
      request.subScriptionCreate.businessDetail.soleProprietor shouldNot(be(None))

      val solePoprietor = request.subScriptionCreate.businessDetail.soleProprietor.get
      solePoprietor.identification.nino shouldEqual Some("AA123111A")
      solePoprietor.identification.vatRegistrationNumber shouldEqual Some("123456789")
      solePoprietor.identification.uniqueTaxpayerReference shouldEqual Some("1111111112")

      request.subScriptionCreate.additionalBusinessInformation.partnerCorporateBody shouldEqual None

    }


    "Create a correct json for sole-proprietor-minimum.xml" in {
      validatesFor("sole-proprietor-minimum.xml")
    }

    "Create a correct json for sole-proprietor-minimum-international.xml" in {
      val request = validatesFor("sole-proprietor-minimum-international.xml")

      request.subScriptionCreate.contactDetail.address.map(_.countryCode) shouldEqual Some("AL")
      request.subScriptionCreate.contactDetail.address.flatMap(_.line4) shouldEqual Some("Albania")
    }


  }

  def validatesFor(file: String): SubScriptionCreate = {
    val iform = loadSubmission(file)
    val subscrtiptionCreate = service.iformXmlToSubmission(iform, brd)


    val json = Json.toJson(subscrtiptionCreate)

    val validationResult = validator(json)
    validationResult.fold(
      invalid = {errors ⇒ println(errors.toJson)},
      valid = {v ⇒ }
    )

    validationResult.isSuccess shouldEqual true

    val expected = loadExpectedSubscriptionForFile(file)
    subscrtiptionCreate shouldEqual expected

    subscrtiptionCreate
  }

  def loadExpectedSubscriptionForFile(file: String): SubScriptionCreate = {
    val baseName = FilenameUtils getBaseName file
    val resource = getClass.getResourceAsStream(s"/json/valid/sole-proprietor/$baseName.json")
    Json.parse(resource).as[SubScriptionCreate]
  }

  def loadSubmission(file: String): generated.sole.Data = {
    val xml = XML
      .load(getClass.getResourceAsStream(s"/xml/valid/sole-proprietor/$file"))
    scalaxb.fromXML[generated.sole.Data](xml)
  }

}
