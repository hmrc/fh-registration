package uk.gov.hmrc.fhdds.services

trait BusinessInformationService {

  def getBusinessRegistrationDetails(formSubmissionRef: String)

}
