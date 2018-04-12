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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.fhregistration.connectors._
import uk.gov.hmrc.fhregistration.controllers.FhddsApplicationController
import uk.gov.hmrc.fhregistration.repositories.{SubmissionExtraDataRepository, SubmissionTrackingRepository}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.HeaderCarrierConverter

object FhddsApplicationControllerMock extends ScalaFutures with MockitoSugar {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(10, Millis))

  var mockDesConnector: DesConnector = mock[DesConnectorImpl]
  var mockTaxEnrolmentConnector: TaxEnrolmentConnector = mock[TaxEnrolmentConnectorImpl]
  var mockEmailConnectorImplConnector: EmailConnector = mock[EmailConnectorImpl]
  var mockSubmissionTrackingRepository: SubmissionTrackingRepository = mock[SubmissionTrackingRepository]
  var mockFhddsApplicationService: FhddsApplicationService = new FhddsApplicationServiceImpl(new CountryCodeLookupImpl)
  var auditService: AuditService = new AuditServiceImpl

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(HeaderNames.xSessionId -> "test")
  implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

  var fhddsApplicationController = new FhddsApplicationController(
    mockDesConnector,
    mockTaxEnrolmentConnector,
    mockEmailConnectorImplConnector,
    mockSubmissionTrackingRepository,
    mockFhddsApplicationService,
    auditService
  )
}
