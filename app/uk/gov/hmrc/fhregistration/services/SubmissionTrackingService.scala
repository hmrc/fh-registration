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

package uk.gov.hmrc.fhregistration.services

import java.time.Clock
import javax.inject.Inject

import cats.data.OptionT
import com.google.inject.ImplementedBy
import play.api.Logger
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress
import uk.gov.hmrc.fhregistration.models.fhdds.EnrolmentProgress.EnrolmentProgress
import uk.gov.hmrc.fhregistration.repositories.{SubmissionTracking, SubmissionTrackingRepository}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._

@ImplementedBy(classOf[DefaultSubmissionTrackingService])
trait SubmissionTrackingService {
  def enrolmentProgress(userId: String, registrationNumber: Option[String]): Future[EnrolmentProgress]
  def saveSubscriptionTracking(safeId: String, userId: String, etmpFormBundleNumber: String, emailAddress: String, registrationNumber: String): Future[_]
  def updateSubscriptionTracking(etmpFormBundleNumber: String, enrolmentProgress: EnrolmentProgress): Future[_]
  def getSubmissionTrackingEmail(formBundleId: String): OptionT[Future, String]
  def deleteSubmissionTracking(formBundleId: String): Future[_]
}

class DefaultSubmissionTrackingService @Inject()(
  repository: SubmissionTrackingRepository,
  clock: Clock)
  extends SubmissionTrackingService {
  val SubmissionTrackingAgeThresholdMs = 60 * 60 * 1000L

  override def enrolmentProgress(userId: String, registrationNumber: Option[String]): Future[EnrolmentProgress] = {
    val now = clock.millis()
    for {
      _ ← clearSubmissionTrackingForRegNumber(userId, registrationNumber)
      tracking ← repository.findSubmissionTrackingByUserId(userId)
    } yield {
      tracking match {
        case Some(tracking) ⇒
          if (tracking.enrolmentProgress == EnrolmentProgress.Pending && (now - tracking.submissionTime) > SubmissionTrackingAgeThresholdMs) {
            Logger.error(s"Submission tracking is too old for user $userId. Was made at ${tracking.submissionTime}")
            EnrolmentProgress.Error
          } else {
            tracking.enrolmentProgress
          }
        case None                                                                                 ⇒
          EnrolmentProgress.Unknown
      }
    }
  }

  private def clearSubmissionTrackingForRegNumber(userId: String, registrationNumber: Option[String]): Future[Int] = {
    registrationNumber.fold(Future successful 0) { r ⇒
      repository.deleteSubmissionTackingByRegistrationNumber(userId, r)
    }
  }

  override def saveSubscriptionTracking(safeId: String, userId: String, etmpFormBundleNumber: String, emailAddress: String, registrationNumber: String): Future[_] = {
    val submissionTracking = SubmissionTracking(
      userId,
      etmpFormBundleNumber,
      emailAddress,
      clock.millis(),
      EnrolmentProgress.Pending,
      registrationNumber
    )

    val result = repository.insertSubmissionTracking(submissionTracking)
    result
      .map { _ ⇒ Logger.info(s"Submission tracking record saved for $safeId and etmpFormBundleNumber $etmpFormBundleNumber")}
      .recover { case error ⇒
        Logger.error(s"Submission tracking record FAILED for $safeId and etmpFormBundleNumber $etmpFormBundleNumber", error)
      }
  }

  override def updateSubscriptionTracking(etmpFormBundleNumber: String, enrolmentProgress: EnrolmentProgress): Future[_] = {
    val result = repository updateEnrolmentProgress (etmpFormBundleNumber, enrolmentProgress)
    result
      .map(_ ⇒ Logger.info(s"Submission tracking record saved for etmpFormBundleNumber $etmpFormBundleNumber"))
      .recover { case error ⇒
        Logger.error(s"Submission tracking record FAILED for etmpFormBundleNumber $etmpFormBundleNumber", error)
      }
  }

  override def getSubmissionTrackingEmail(formBundleId: String): OptionT[Future, String] = {
    OptionT(repository.findSubmissionTrakingByFormBundleId(formBundleId)).map(_.email)
  }

  override def deleteSubmissionTracking(formBundleId: String): Future[_] = {
    repository
      .deleteSubmissionTackingByFormBundleId(formBundleId)
      .andThen {
        case Success(1) ⇒ Logger.info(s"Submission tracking deleted for $formBundleId")
        case Success(0) ⇒ Logger.warn(s"Submission tracking not found for $formBundleId")
        case Success(n) ⇒ Logger.error(s"Submission tracking delete for $formBundleId returned an unexpected number of docs: $n")
        case Failure(e) ⇒ Logger.error(s"Submission tracking delete failed for $formBundleId", e)
      }
  }


}
