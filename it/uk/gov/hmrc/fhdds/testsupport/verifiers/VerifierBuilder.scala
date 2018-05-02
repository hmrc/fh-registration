package uk.gov.hmrc.fhdds.testsupport.verifiers


class VerifierBuilder {
  implicit val builder: VerifierBuilder = this

  lazy val des = DesVerifier()
  lazy val taxEnrolments = TaxEnrolmentsVerifier()
  lazy val email = EmailVerifier()

}
