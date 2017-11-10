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

import uk.gov.hmrc.play.test.UnitSpec

import scala.xml.XML

class IFormXmlParserSpec extends UnitSpec {


  "test xml parsing" should {
    "parse without errors shortest form" in {
      val file = "fhdds-limited-company-minimum.xml"
      val x = XML.load(getXmlInputStream(s"valid/$file"))
      scalaxb.fromXML[generated.Data](x) should not be null

    }

//    "parse without errors short form with international contact address" in {
//      val file = "fhdds-limited-company-minimum-international.xml"
//      val x = XML.load(getXmlInputStream(s"valid/$file"))
//      scalaxb.fromXML[generated.Data](x) should not be null
//
//    }

//    "parse without errors long form with uk contact address" in {
//      val file = "fhdds-limited-company-large-uk.xml"
//      val x = XML.load(getXmlInputStream(s"valid/$file"))
//      scalaxb.fromXML[generated.Data](x) should not be null
//
//    }

  //
  //    "parse without errors example-maximum.xml" in {
  //      val file = "example-maximum.xml"
  //      val x = XML.loadFile(s"$basePath/$file")
  //      scalaxb.fromXML[generated.Data](x) should not be null
  //
  //    }
    }

  def getXmlInputStream(name: String) = {
    getClass.getResourceAsStream(s"/xml/$name")
  }


}
