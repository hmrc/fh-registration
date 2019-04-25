/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.testsupport.mocks

import java.time.Clock

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.fhregistration.actions.Actions
import uk.gov.hmrc.fhregistration.connectors._
import uk.gov.hmrc.fhregistration.controllers.FhddsApplicationController
import uk.gov.hmrc.fhregistration.repositories.{DefaultSubmissionTrackingRepository, SubmissionTracking, SubmissionTrackingRepository}
import uk.gov.hmrc.fhregistration.services.{AuditService, DefaultSubmissionTrackingService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import org.mockito.ArgumentMatchers.any
import reactivemongo.api.ReadPreference
import org.mockito.Mockito.when

import scala.concurrent.{ExecutionContext, Future}


trait FhddsMocks extends ScalaFutures with MockitoSugar {

  //  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(10, Millis))

  val mockDesConnector: DesConnector = mock[DesConnector]
  val mockTaxEnrolmentConnector: TaxEnrolmentConnector = mock[TaxEnrolmentConnector]
  val mockEmailConnectorConnector: EmailConnector = mock[EmailConnector]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockSubmissionTrackingRepository: SubmissionTrackingRepository = mock[SubmissionTrackingRepository]
  val mockSubmissionTrackingService = new DefaultSubmissionTrackingService(mockSubmissionTrackingRepository, Clock.systemDefaultZone())
  val mockActions = mock[Actions]
  val mockRepository = mock[DefaultSubmissionTrackingRepository]
  when(mockRepository.findAll(any[ReadPreference])(any[ExecutionContext])) thenReturn
    Future.successful(List(new SubmissionTracking("1", "2", "3", 0, None, None)))
//  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(HeaderNames.xSessionId -> "test")`
//  implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)


  var fhddsApplicationControllerWithMocks = new FhddsApplicationController(
    mockDesConnector,
    mockTaxEnrolmentConnector,
    mockEmailConnectorConnector,
    mockSubmissionTrackingService,
    mockAuditService,
    mockAuditConnector,
    mockActions,
    mockRepository
  )
}
