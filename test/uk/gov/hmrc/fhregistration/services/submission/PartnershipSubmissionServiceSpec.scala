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
import generated.fhdds.PartnershipDataFormat
import org.apache.commons.io.FilenameUtils
import play.api.libs.json.Json
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate
import uk.gov.hmrc.fhregistration.models.des.SubScriptionCreate.format
import uk.gov.hmrc.fhregistration.services.CountryCodeLookupImpl
import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class PartnershipSubmissionServiceSpec extends UnitSpec {

  val schemaAsJson = Json parse getClass.getResourceAsStream("/schemas/des-schema-r1.json")
  val schema = Json.fromJson[SchemaType](schemaAsJson).get
  val validator = SchemaValidator().validate(schema) _
  val service = new PartnershipSubmissionService(new CountryCodeLookupImpl)

  val brd: BusinessRegistrationDetails = Json
    .parse(getClass.getResourceAsStream("/models/business-registration-details-partnership.json"))
    .as[BusinessRegistrationDetails]

  "Partnership submission service" should {

    "Create a correct json for partnership-minimum" in {
      val request = validatesFor("partnership-minimum.xml")

      request.subScriptionCreate.businessDetail.nonProprietor should not be None
      request.subScriptionCreate.businessDetail.nonProprietor.flatMap(_.identification.uniqueTaxpayerReference) shouldBe Some("1111111113")

    }

    "Create a correct json for partnership-large-int" in {
      val request = validatesFor("partnership-large-int.xml")

      request.subScriptionCreate.businessDetail.nonProprietor should not be None
      request.subScriptionCreate.businessDetail.nonProprietor.flatMap(_.identification.uniqueTaxpayerReference) shouldBe Some("1111111113")

      request.subScriptionCreate.businessDetail.partnership.map(_.numbersOfPartners) shouldBe Some("6")

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
    val resource = getClass.getResourceAsStream(s"/json/valid/partnership/$baseName.json")
    Json.parse(resource).as[SubScriptionCreate]
  }

  def loadSubmission(file: String): generated.partnership.Data = {
    val xml = XML
      .load(getClass.getResourceAsStream(s"/xml/valid/partnership/$file"))
    scalaxb.fromXML[generated.partnership.Data](xml)
  }

}
