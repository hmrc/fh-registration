package uk.gov.hmrc.fhregistration

import play.api.test.WsTestClient
import uk.gov.hmrc.fhdds.testsupport.{TestConfiguration, TestHelpers}

class MdtpSubscriptionStatusSpecs
  extends TestHelpers with TestConfiguration {

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
            .url(s"http://localhost:$port/fhdds/subscription/$receivedRegistrationNumber/status")
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
            .url(s"http://localhost:$port/fhdds/subscription/$processingRegistrationNumber/status")
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
            .url(s"http://localhost:$port/fhdds/subscription/$successfulRegistrationNumber/status")
            .get().futureValue
        }
        response.status shouldBe 200
        response.body shouldBe "Successful"
      }

    }
  }

}
