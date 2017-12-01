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

import generated.{PanelCompany, PanelPerson}
import org.apache.commons.lang3.StringUtils
import uk.gov.hmrc.fhdds.models.des._

object ApplicationUtils {


  def getOrderType(fulfilmentOrdersType: generated.FulfilmentOrdersType): FulfilmentOrdersType = {
    FulfilmentOrdersType(
      onLine = isYes(fulfilmentOrdersType.onLine),
      telephone = isYes(fulfilmentOrdersType.telephone),
      physicalPremises = isYes(fulfilmentOrdersType.physicalPremises),
      other = isYes(fulfilmentOrdersType.other),
      typeOfOtherOrder =
        if (isYes(fulfilmentOrdersType.other))
          fulfilmentOrdersType.panelTypeOfOtherOrder.map(_.typeOfOtherOrder)
        else
          None
    )

  }


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

  def isYes(radioButtonAnswer: String): Boolean = radioButtonAnswer equals "Yes"

  def getCompanyOfficialAsPerson(personPanel: PanelPerson): CompanyOfficial = {
    IndividualAsOfficial(
      role = personPanel.role match {
        case "Secretary"          ⇒ "Company Secretary"
        case "Director+Secretary" ⇒ "Director and Company Secretary"
        case "Director"           ⇒ "Director"
        case _                    ⇒ "Member"
      },
      name = Name(firstName = personPanel.firstName,
        middleName = None,
        lastName = personPanel.lastName),
      identification = {
        if (isYes(personPanel.hasNino)) {
          IndividualIdentification(
            nino = personPanel.panelNino.map(_.nino),
            passportNumber = None,
            nationalIdNumber = None
          )
        } else {
          IndividualIdentification(
            nino = None,
            passportNumber = personPanel.panelNoNino.flatMap(
              personNoNino ⇒ if (isYes(personNoNino.hasPassportNumber)) {
                personNoNino.panelPassportNumber.map(
                  passportNumber ⇒ passportNumber.passportNumber
                )
              } else None
            ),
            nationalIdNumber = personPanel.panelNoNino.flatMap(
              personNoNino ⇒
                personNoNino.panelNationalIDNumber.map(_.nationalIdNumber)
            )
          )
        }
      }
    )
  }

  def getCompanyOfficialAsCompany(companyPanel: PanelCompany): CompanyOfficial = {
    CompanyAsOfficial(
      role = companyPanel.role match {
          case "Secretary"          ⇒ "Company Secretary"
          case "Director+Secretary" ⇒ "Director and Company Secretary"
          case "Director"           ⇒ "Director"
          case _                    ⇒ "Member"
        },
      name = CompanyName(companyName = Some(companyPanel.companyName)),
      identification = {
        if (isYes(companyPanel.hasVat)) {
          CompanyIdentification(
            vatRegistrationNumber = companyPanel.panelHasVat.map(_.vatRegistrationNumber),
            uniqueTaxpayerReference = None,
            companyRegistrationNumber = None
          )
        } else {
          CompanyIdentification(
            vatRegistrationNumber = None,
            uniqueTaxpayerReference = None,
            companyRegistrationNumber = companyPanel.panelCrn.map(_.companyRegistrationNumber)
          )
        }
      }
    )
  }
}