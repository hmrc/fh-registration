/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.models.fhdds

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsSuccess, Json}

class SendEmailRequestSpec extends AnyFunSuite {

  test("SendEmailRequest should serialize and deserialize correctly") {
    val to = List("test1@email.com", "test2@email.com")
    val templateId = "test template"
    val parameters = Map("param1" -> "value1", "param2" -> "value2")
    val force = true
    val sendEmailRequest = SendEmailRequest(to, templateId, parameters, force)

    val json = Json.toJson(sendEmailRequest)
    assert((json \ "to").as[List[String]] == to)
    assert((json \ "templateId").as[String] == templateId)
    assert((json \ "parameters").as[Map[String, String]] == parameters)
    assert((json \ "force").as[Boolean] == force)

    val deserialized = json.validate[SendEmailRequest]
    assert(deserialized == JsSuccess(sendEmailRequest))
  }
}
