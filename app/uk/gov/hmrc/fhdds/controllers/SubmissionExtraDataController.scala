package uk.gov.hmrc.fhdds.controllers

import javax.inject.Inject

import play.api.mvc.Action
import uk.gov.hmrc.fhdds.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhdds.repositories.SubmissionExtraDataRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.libs.json.Json

class SubmissionExtraDataController @Inject() (
  val submissionDataRepository: SubmissionExtraDataRepository)
  extends BaseController {


  def saveBusinessRegistrationDetails(userId: String, formTypeRef: String) = Action.async(parse.json[BusinessRegistrationDetails]) {
    request ⇒
      submissionDataRepository
        .saveBusinessRegistrationDetails(userId, formTypeRef, request.body)
        .map(result ⇒
          if(result.ok) Ok()
          else BadGateway(Json.toJson(s"Mongo Db error ${result.message}"))
        ).recover{case e ⇒ BadGateway(Json.toJson(s"Mongo Db error ${e.getMessage}"))}
  }

  

}
