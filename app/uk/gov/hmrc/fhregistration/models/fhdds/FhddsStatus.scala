/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.models.fhdds

import play.api.libs.json.{Format, Reads, Writes}

object FhddsStatus extends Enumeration {

  type FhddsStatus = Value

  val Processing = Value("processing")
  val Received = Value("received")
  val Approved = Value("approved")
  val ApprovedWithConditions = Value("approvedWithConditions")
  val Rejected = Value("rejected")
  val Revoked = Value("revoked")
  val Withdrawn = Value("withdrawn")
  val Deregistered = Value("deregistered")

  implicit val format: Format[Value] = Format(Reads.enumNameReads(FhddsStatus), Writes.enumNameWrites[this.type])

}
