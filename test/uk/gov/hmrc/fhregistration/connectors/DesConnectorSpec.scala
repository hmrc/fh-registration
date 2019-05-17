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

package uk.gov.hmrc.fhregistration.connectors


import com.typesafe.config.Config
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global

class DesConnectorSpec extends UnitSpec with MockitoSugar{
  class DefaultDesConnectorMock(
                                 val httpClient: HttpClient,
                                 val runModeConfig: Configuration,
                                 runMode: RunMode,
                                 environment: Environment,
                                 servicesConfig: ServicesConfig
   ) extends DefaultDesConnector(httpClient, runModeConfig, runMode, environment, servicesConfig) {
    override def baseUrl(serviceName : scala.Predef.String) : scala.Predef.String = serviceName
    override def config(serviceName : scala.Predef.String) : Configuration = new Configuration(mock[Config])
  }

  "URLs and URIs" should {
    val desConnectorMock = new DefaultDesConnectorMock(mock[HttpClient], mock[Configuration], mock[RunMode], mock[Environment], mock[ServicesConfig])

    "desServiceStatusUri" in {
      desConnectorMock.desServiceStatusUri shouldBe "des-service"
    }

    "desSubmissionUrl" in {
      desConnectorMock.desSubmissionUrl("testValue") shouldBe "des-service/id/testValue/id-type/safe"
    }

    "desAmendmentUrl" in {
      desConnectorMock.desAmendmentUrl("testValue") shouldBe "des-service/id/testValue/id-type/fhdds"
    }

    "desWithdrawalUrl" in {
      desConnectorMock.desWithdrawalUrl("testValue") shouldBe "des-service/testValue/withdrawal"
    }

    "desDeregisterUrl" in {
      desConnectorMock.desDeregisterUrl("testValue") shouldBe "des-service/testValue/deregistration"
    }
  }
}