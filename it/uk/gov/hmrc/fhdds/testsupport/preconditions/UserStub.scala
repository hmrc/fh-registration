package uk.gov.hmrc.fhdds.testsupport.preconditions

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.fhdds.testsupport.TestData

case class UserStub()(implicit builder: PreconditionBuilder) { //extends SessionBuilder {

//  def isAuthorised(implicit requestHolder: RequestHolder): PreconditionBuilder = {
//    requestHolder.request = requestWithSession(requestHolder.request, "anyUserId")
//    stubFor(
//      get(urlPathEqualTo("/auth/authority"))
//        .willReturn(ok(
//          s"""
//             |{
//             |  "uri":"anyUserId",
//             |  "loggedInAt": "2014-06-09T14:57:09.522Z",
//             |  "previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
//             |  "credentials": {"gatewayId":"xxx2"},
//             |  "accounts": {},
//             |  "levelOfAssurance": "2",
//             |  "confidenceLevel" : 50,
//             |  "credentialStrength": "strong",
//             |  "legacyOid": "1234567890",
//             |  "userDetailsLink": "http://localhost:11111/auth/userDetails",
//             |  "ids": "/auth/ids"
//             |}""".stripMargin
//        )))
//    builder
//  }

  def isAuthorisedWithNoGroup() = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          ok(
            s"""
               |{
               |  "internalId": "${TestData.testUserId}",
               |  "loginTimes": {
               |     "currentLogin": "2016-11-27T09:00:00.000Z",
               |     "previousLogin": "2016-11-01T12:00:00.000Z"
               |  }
               |}
             """.stripMargin
          )
        )
    )
    builder

  }

  def isAuthorised() = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          ok(
            s"""
               |{
               |  "internalId": "${TestData.testUserId}",
               |  "groupIdentifier": "some-group",
               |  "loginTimes": {
               |     "currentLogin": "2016-11-27T09:00:00.000Z",
               |     "previousLogin": "2016-11-01T12:00:00.000Z"
               |  }
               |}
             """.stripMargin
          )
        )
    )
    builder

  }

  def isAuthorisedAndEnrolled = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(
          ok(
            s"""
               |{
               |  "internalId": "${TestData.testUserId}",
               |  "email": "test@test.com",
               |  "allEnrolments": [{
               |     "key": "HMRC-OBTDS-ORG",
               |     "identifiers": [{
               |       "key":"ETMPREGISTRATIONNUMBER",
               |       "value": "XEFH01234567890"
               |     }]
               |  }],
               |  "loginTimes": {
               |     "currentLogin": "2018-03-27T09:00:00.000Z",
               |     "previousLogin": "2018-03-01T12:00:00.000Z"
               |  }
               |}
             """.stripMargin
          )
        )
    )
    builder
  }


  def isNotAuthorised(reason: String = "MissingBearerToken") = {
    stubFor(
      post(urlPathEqualTo("/auth/authorise"))
        .willReturn(unauthorized().withHeader("WWW-Authenticate", s"""MDTP detail="$reason"""")))

    builder
  }
}