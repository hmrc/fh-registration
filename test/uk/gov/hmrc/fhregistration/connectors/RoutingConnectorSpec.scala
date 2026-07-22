/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.connectors

import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.fhregistration.models.des.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.util.Date
import scala.concurrent.Future

class RoutingConnectorSpec extends PlaySpec with MockitoSugar {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    val mockHipConnector: HipConnector = mock[HipConnector]
    val mockDesConnector: DesConnector = mock[DesConnector]
  }

  "RoutingConnector" when {
    val json = Json.obj("x" -> "y")
    val fhddsNo = "some-fhddsNo"
    val bundleNo = "some-etmpFormBundleNumber"
    val processDate = new Date(System.currentTimeMillis())
    val okResponse = HttpResponse(OK, json, Map())

    Seq(("DES", false), ("HIP", true)).foreach { case (etmp, useHip) =>
      val configuration = Configuration.from(Map("feature.hip" -> useHip))

      s"useHip flag is $useHip" must {

        s"perform 'Display' by delegating to the $etmp connector" in new Setup {
          if (useHip)
            when(mockHipConnector.subscriptionDisplay(any())(eqTo(hc)))
              .thenReturn(Future.successful(okResponse))
          else
            when(mockDesConnector.display(any())(eqTo(hc)))
              .thenReturn(Future.successful(okResponse))

          val routingConnector = new RoutingConnector(configuration, mockDesConnector, mockHipConnector)
          routingConnector.display(fhddsNo)

          if (useHip)
            verify(mockHipConnector, times(1)).subscriptionDisplay(eqTo(fhddsNo))(eqTo(hc))
          else
            verify(mockDesConnector, times(1)).display(eqTo(fhddsNo))(eqTo(hc))
        }

        s"perform 'sendDeregistration' by delegating to the $etmp connector" in new Setup {
          val deregResponse = DesDeregistrationResponse(processDate)
          if (useHip)
            when(mockHipConnector.subscriptionDeregistration(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(deregResponse))
          else
            when(mockDesConnector.sendDeregistration(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(deregResponse))

          val routingConnector = new RoutingConnector(configuration, mockDesConnector, mockHipConnector)
          routingConnector.sendDeregistration(fhddsNo, json)

          if (useHip)
            verify(mockHipConnector, times(1)).subscriptionDeregistration(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
          else
            verify(mockDesConnector, times(1)).sendDeregistration(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
        }

        s"perform 'sendWithdrawal' by delegating to the $etmp connector" in new Setup {
          val withdrawalResponse = DesWithdrawalResponse(processDate)
          if (useHip)
            when(mockHipConnector.subscriptionWithdrawal(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(withdrawalResponse))
          else
            when(mockDesConnector.sendWithdrawal(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(withdrawalResponse))

          val routingConnector = new RoutingConnector(configuration, mockDesConnector, mockHipConnector)
          routingConnector.sendWithdrawal(fhddsNo, json)

          if (useHip)
            verify(mockHipConnector, times(1)).subscriptionWithdrawal(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
          else
            verify(mockDesConnector, times(1)).sendWithdrawal(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
        }

        s"perform 'sendSubmission' by delegating to the $etmp connector" in new Setup {
          val submissionResponse = DesSubmissionResponse(processDate, bundleNo, fhddsNo)
          if (useHip)
            when(mockHipConnector.createOrUpdateFhdds(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(submissionResponse))
          else
            when(mockDesConnector.sendSubmission(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(submissionResponse))

          val routingConnector = new RoutingConnector(configuration, mockDesConnector, mockHipConnector)
          routingConnector.sendSubmission(fhddsNo, json)

          if (useHip)
            verify(mockHipConnector, times(1)).createOrUpdateFhdds(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
          else
            verify(mockDesConnector, times(1)).sendSubmission(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
        }
        s"perform 'sendAmendment' by delegating to the $etmp connector" in new Setup {
          val submissionResponse = DesSubmissionResponse(processDate, bundleNo, fhddsNo)
          if (useHip)
            when(mockHipConnector.createOrUpdateFhdds(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(submissionResponse))
          else
            when(mockDesConnector.sendAmendment(any(), any())(eqTo(hc)))
              .thenReturn(Future.successful(submissionResponse))

          val routingConnector = new RoutingConnector(configuration, mockDesConnector, mockHipConnector)
          routingConnector.sendAmendment(fhddsNo, json)

          if (useHip)
            verify(mockHipConnector, times(1)).createOrUpdateFhdds(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
          else
            verify(mockDesConnector, times(1)).sendAmendment(eqTo(fhddsNo), eqTo(json))(eqTo(hc))
        }
      }
    }
    s"perform 'getStatus' by delegating to the DES connector" in new Setup {
      val configuration = Configuration.from(Map("feature.hip" -> false))
      val statusResponse = StatusResponse
      when(mockDesConnector.getStatus(any())(eqTo(hc)))
        .thenReturn(Future.successful(statusResponse))

      val routingConnector = new RoutingConnector(configuration, mockDesConnector, mockHipConnector)
      routingConnector.getStatus(fhddsNo)
      verify(mockDesConnector, times(1)).getStatus(eqTo(fhddsNo))(eqTo(hc))
    }
  }

}
