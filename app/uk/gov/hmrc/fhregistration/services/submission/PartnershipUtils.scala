/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.fhregistration.services.submission

import generated.partnership._
import uk.gov.hmrc.fhregistration.models.des._
import uk.gov.hmrc.fhregistration.services.submission.SubmissionUtils._

object PartnershipUtils {

  def partner(panel: PanelRepeatingPartner): PartnerType = {
    panel.entityType match {
      case "Sole Proprietor"               => soleProprietor(panel.panelSoleProprietor.get)
      case "Limited Liability Partnership" => company(panel.panelCompany.get)
      case "Partnership"                   => partnership(panel.panelPartnership.get)
      case "Corporate Body"                => company(panel.panelCompany.get)
      case "Unincorporated Body"           => unincorporatedBody(panel.panelUnincorporated.get)
      case "Individual"                    => individual(panel.panelIndividual.get)
    }
  }

  def unincorporatedBody(panel: PanelUnincorporated): PartnershipOrUnIncorporatedBodyPartnerType = {
    PartnershipOrUnIncorporatedBodyPartnerType(
      names = CompanyName(
        Some(panel.companyName),
        whenYes(
          panel.hasTradingName,
          panel.panelTradingName.map(_.tradingName))),

      identification = PartnerIdentification(
        vatRegistrationNumber = whenYes(
          panel.hasVAT,
          panel.panelVAT.map(_.vatRegistrationNumber)),
        uniqueTaxpayerReference = whenYes(
          panel.hasUTR,
          panel.panelUTR.map(_.uniqueTaxpayerReference))
      )
    )
  }

  def partnership(panel: PanelPartnership): PartnershipOrUnIncorporatedBodyPartnerType = {
    PartnershipOrUnIncorporatedBodyPartnerType(
      names = CompanyName(
        Some(panel.companyName),
        tradingName =  whenYes(
          panel.hasTradingName,
          panel.panelTradingName.map(_.tradingName))),

      identification = PartnerIdentification(
        vatRegistrationNumber = whenYes(
          panel.hasVAT,
          panel.panelVAT.map(_.vatRegistrationNumber)),
        uniqueTaxpayerReference = whenYes(
          panel.hasUTR,
          panel.paneUTR.map(_.uniqueTaxpayerReference))
      )
    )
  }

  def individual(panel: PanelIndividual): IndividualPartnerType = {
    IndividualPartnerType(
      name = Name(
        panel.firstName,
        None,
        panel.lastName),

      nino = whenYes(
        panel.hasNino,
        panel.panelNino.map(_.nino))
    )
  }

  def soleProprietor(panel: PanelSoleProprietor): SoleProprietorPartnerType = {
    SoleProprietorPartnerType(
      name = Name(
        panel.firstName,
        None,
        panel.lastName),

      nino = whenYes(
        panel.hasNino,
        panel.panelNino.map(_.nino)),

      identification = PartnerIdentification(
        vatRegistrationNumber = whenYes(
          panel.panelIdentification.hasVAT,
          panel.panelIdentification.panelVAT.map(_.vatRegistrationNumber)),
        uniqueTaxpayerReference =
          if (!isYes(panel.panelIdentification.hasVAT))
            panel.panelIdentification.panelUTR.map(_.uniqueTaxpayerReference)
          else
            None
      ),
      tradingName =  whenYes(
        panel.hasTradingName,
        panel.panelTradingName.map(_.tradingName))
    )
  }

  def company(panel: PanelCompany): LimitedLiabilityPartnershipType = {
    LimitedLiabilityPartnershipType(
      names = CompanyName(
        Some(panel.companyName),
        tradingName =  whenYes(
          panel.hasTradingName,
          panel.panelTradingName.map(_.tradingName))),

      identification = PartnerIdentification(
        vatRegistrationNumber = whenYes(
          panel.hasVAT,
          panel.panelVAT.map(_.vatRegistrationNumber)),
        uniqueTaxpayerReference =
          if (!isYes(panel.hasVAT))
            panel.panelUTR.map(_.uniqueTaxpayerReference)
          else None
      ),

      incorporationDetails = IncorporationDetail(
        companyRegistrationNumber = Some(panel.companyRegistrationNumber),
        None
      )
    )

  }



}
