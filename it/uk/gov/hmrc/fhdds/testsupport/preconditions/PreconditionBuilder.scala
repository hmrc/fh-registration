package uk.gov.hmrc.fhdds.testsupport.preconditions

class PreconditionBuilder {
  implicit val builder: PreconditionBuilder = this

  def audit = AuditStub()
  def user = UserStub()
  def des = DesStub()
  def taxEnrolment = TaxEnrolmentStub()

}
