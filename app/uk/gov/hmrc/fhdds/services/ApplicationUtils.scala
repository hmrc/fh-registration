/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.fhdds.services

import org.apache.commons.lang3.StringUtils

object ApplicationUtils {

  implicit class AddressLineUtils(value: Option[String]) {

    /** Transforms Some("") in None */
    def noneIfBlank = value flatMap { s ⇒
      if (StringUtils isBlank s)
        None
      else
        value
    }

    /** Returns " " for None */
    def nonEmptyString = value.noneIfBlank getOrElse " "
  }

}