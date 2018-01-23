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

package uk.gov.hmrc.fhregistration.models.iform

import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class IFormXmlParserSpec extends UnitSpec {
  import generated.fhdds.SoleDataFormat
  import generated.fhdds.LimitedDataFormat
  import generated.fhdds.PartnershipDataFormat

  "test xml parsing for limited company" should {
    "parse without errors shortest form" in {
      val file = "fhdds-limited-company-minimum.xml"
      val x = XML.load(getXmlInputStream(s"valid/limited-company/$file"))
      scalaxb.fromXML[generated.limited.Data](x) should not be null

    }

    "parse without errors shortest form with gg email" in {
      val file = "fhdds-limited-company-minimum-with-ggemail.xml"
      val x = XML.load(getXmlInputStream(s"valid/limited-company/$file"))
      scalaxb.fromXML[generated.limited.Data](x) should not be null

    }


    "parse without errors short form with international contact address" in {
      val file = "fhdds-limited-company-minimum-international.xml"
      val x = XML.load(getXmlInputStream(s"valid/limited-company/$file"))
      scalaxb.fromXML[generated.limited.Data](x) should not be null

    }

    "parse without errors long form with uk contact address" in {
      val file = "fhdds-limited-company-large-uk.xml"
      val x = XML.load(getXmlInputStream(s"valid/limited-company/$file"))
      scalaxb.fromXML[generated.limited.Data](x) should not be null

    }

  }

  "test xml parsing for sole proprietor" should {
    "parse without errors shortest form" in {
      val file = "sole-proprietor-minimum.xml"
      val x = XML.load(getXmlInputStream(s"valid/sole-proprietor/$file"))
      scalaxb.fromXML[generated.sole.Data](x) should not be null

    }

    "parse without errors shortest form with international address" in {
      val file = "sole-proprietor-minimum-international.xml"
      val x = XML.load(getXmlInputStream(s"valid/sole-proprietor/$file"))
      scalaxb.fromXML[generated.sole.Data](x) should not be null

    }

    "parse without errors long form with uk address" in {
      val file = "sole-proprietor-large-uk.xml"
      val x = XML.load(getXmlInputStream(s"valid/sole-proprietor/$file"))
      scalaxb.fromXML[generated.sole.Data](x) should not be null

    }

    "parse without errors long form with third party storage" in {
      val file = "sole-proprietor-third-party-storage.xml"
      val x = XML.load(getXmlInputStream(s"valid/sole-proprietor/$file"))
      scalaxb.fromXML[generated.sole.Data](x) should not be null

    }
  }

  "test xml parsing for partnership" should {
    "parse without errors shortest form" in {
      val file = "partnership-minimum.xml"
      val x = XML.load(getXmlInputStream(s"valid/partnership/$file"))
      scalaxb.fromXML[generated.partnership.Data](x) should not be null

    }

    "parse without errors longer form" in {
      val file = "partnership-large-int.xml"
      val x = XML.load(getXmlInputStream(s"valid/partnership/$file"))
      scalaxb.fromXML[generated.partnership.Data](x) should not be null

    }
  }

  def getXmlInputStream(name: String) = {
    getClass.getResourceAsStream(s"/xml/$name")
  }


}
