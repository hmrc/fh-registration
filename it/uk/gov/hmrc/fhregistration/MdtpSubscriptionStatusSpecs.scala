package uk.gov.hmrc.fhregistration

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, OptionValues, WordSpec, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import play.api.libs.json.Json
import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.TestData._
import uk.gov.hmrc.fhdds.testsupport.TestedApplication

class MdtpSubscriptionStatusSpecs
  extends WordSpec
    with OptionValues
    with WsScalaTestClient
    with TestedApplication
    with WordSpecLike
    with Matchers
    with ScalaFutures {

  val receivedRegistrationNumber = "receivedRegistrationNumber"
  val processingRegistrationNumber = "processingRegistrationNumber"
  val successfulRegistrationNumber = "successfulRegistrationNumber"

  "Retrieve an mdtp subscription status from DES" should {

    "When bta calls fh-registration to retrieve a status and gets a 200 response with status" when {
      "if the the form is received" in {

        given()
          .audit.writesAuditOrMerged()
          .des.getStatus(s"$receivedRegistrationNumber")

        val response = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/fulfilment-diligence/subscription/$receivedRegistrationNumber/status")
            .get().futureValue
        }
        response.status shouldBe 200
        response.body shouldBe "Received"
      }

      "if the the form is processing" in {

        given()
          .audit.writesAuditOrMerged()
          .des.getStatus(s"$processingRegistrationNumber")

        val response = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/fulfilment-diligence/subscription/$processingRegistrationNumber/status")
            .get().futureValue
        }
        response.status shouldBe 200
        response.body shouldBe "Processing"
      }

      "if the the form is successful" in {

        given()
          .audit.writesAuditOrMerged()
          .des.getStatus(s"$successfulRegistrationNumber")

        val response = WsTestClient.withClient { client ⇒
          client
            .url(s"http://localhost:$port/fhdds/fulfilment-diligence/subscription/$successfulRegistrationNumber/status")
            .get().futureValue
        }
        response.status shouldBe 200
        response.body shouldBe "successful"
      }

    }
  }

}
