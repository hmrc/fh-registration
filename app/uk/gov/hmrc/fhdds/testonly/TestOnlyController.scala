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

package uk.gov.hmrc.fhdds.testonly

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraDataRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class TestOnlyController @Inject()(
  val submissionDataRepository: SubmissionExtraDataRepository
)
  extends BaseController {

  def getRegistrationNumber(submissionRef: String) = Action.async {
    Logger.info(s"Getting registration number for $submissionRef")
    submissionDataRepository.findSubmissionExtraDataBySubmissionRef(submissionRef).map(
      _.flatMap(_.registrationNumber) match {
        case Some(registration) ⇒ Ok(Json.toJson(registration))
        case None ⇒ NotFound
      }
    )
  }

}
