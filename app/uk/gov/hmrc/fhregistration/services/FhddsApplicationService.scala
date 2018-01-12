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

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import generated.limited.Data
import generated.sole
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhregistration.services.submission.{LimitedCompanySubmissionService, SoleTraderSubmissionService}

@Singleton
class FhddsApplicationServiceImpl @Inject()(val countryCodeLookup: CountryCodeLookup)
  extends FhddsApplicationService

@ImplementedBy(classOf[FhddsApplicationServiceImpl])
trait FhddsApplicationService {

  protected val countryCodeLookup: CountryCodeLookup
  private val limitedCompanySubmissionService = new LimitedCompanySubmissionService(countryCodeLookup)
  private val soleTraderSubmissionService = new SoleTraderSubmissionService(countryCodeLookup)

  def limitedCompanySubmission(xml: Data, brd: BusinessRegistrationDetails) = limitedCompanySubmissionService.iformXmlToSubmission(xml, brd)
  def soleTraderSubmission(xml: sole.Data, brd: BusinessRegistrationDetails)  = soleTraderSubmissionService.iformXmlToSubmission(xml, brd)
}
