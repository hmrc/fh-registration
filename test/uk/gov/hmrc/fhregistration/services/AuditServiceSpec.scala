/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsString
import play.api.{Configuration, Environment}
import uk.gov.hmrc.fhregistration.models.fhdds.{DeregistrationRequest, SubmissionRequest, WithdrawalRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.fhregistration.util.UnitSpec

class AuditServiceSpec extends UnitSpec with MockitoSugar {
  "buildSubmission methods return correct DataEvent" should {
    val auditService = new DefaultAuditService(mock[HttpClient], mock[Configuration], mock[Environment])
    implicit val hc = new HeaderCarrier()

    "buildSubmissionCreateAuditEvent" in {
      val result = auditService.buildSubmissionCreateAuditEvent(
        new SubmissionRequest("test@test.test", new JsString("jsCreateVal")),
        "someSafeID",
        "testCreateValue"
      )

      result.tags.getOrElse("path", "") shouldBe "/fulfilment-diligence/subscription/someSafeID/id-type/safe"
      result.tags.getOrElse("transactionName", "") shouldBe "FHDDS - testCreateValue"
      result.detail.getOrElse("submissionRef", "") shouldBe "testCreateValue"
      result.detail.getOrElse("submissionData", new JsString("")) shouldBe new JsString("jsCreateVal").toString()
    }

    "buildSubmissionAmendAuditEvent" in {
      val result = auditService.buildSubmissionAmendAuditEvent(
        new SubmissionRequest("test@test.test", new JsString("jsAmendVal")),
        "testAmendValue"
      )

      result.tags.getOrElse("path", "") shouldBe "/fulfilment-diligence/subscription/testAmendValue/id-type/fhdds"
      result.tags.getOrElse("transactionName", "") shouldBe "FHDDS - testAmendValue"
      result.detail.getOrElse("submissionRef", "") shouldBe "testAmendValue"
      result.detail.getOrElse("submissionData", new JsString("")) shouldBe new JsString("jsAmendVal").toString()
    }

    "buildSubmissionWithdrawalAuditEvent" in {
      val result = auditService.buildSubmissionWithdrawalAuditEvent(
        new WithdrawalRequest("test@test.test", new JsString("jsWithdrawalVal")),
        "testWithdrawalValue"
      )

      result.tags.getOrElse("path", "") shouldBe "/fulfilment-diligence/subscription/testWithdrawalValue/withdrawal"
      result.tags.getOrElse("transactionName", "") shouldBe "FHDDS - testWithdrawalValue"
      result.detail.getOrElse("submissionRef", "") shouldBe "testWithdrawalValue"
      result.detail.getOrElse("submissionData", new JsString("")) shouldBe new JsString("jsWithdrawalVal").toString()
    }

    "buildSubmissionDeregisterAuditEvent" in {
      val result = auditService.buildSubmissionDeregisterAuditEvent(
        new DeregistrationRequest("test@test.test", new JsString("jsDeregisterVal")),
        "testDeregisterValue"
      )

      result.tags.getOrElse("path", "") shouldBe "/fulfilment-diligence/subscription/testDeregisterValue/deregister"
      result.tags.getOrElse("transactionName", "") shouldBe "FHDDS - testDeregisterValue"
      result.detail.getOrElse("submissionRef", "") shouldBe "testDeregisterValue"
      result.detail.getOrElse("submissionData", new JsString("")) shouldBe new JsString("jsDeregisterVal").toString()
    }
  }
}
