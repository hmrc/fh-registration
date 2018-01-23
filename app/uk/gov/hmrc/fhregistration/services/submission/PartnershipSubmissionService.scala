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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import generated.partnership
import org.apache.commons.lang3.text.WordUtils
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhregistration.models.des._
import uk.gov.hmrc.fhregistration.services.CountryCodeLookup
import uk.gov.hmrc.fhregistration.services.submission.SubmissionUtils._

class PartnershipSubmissionService(countryCodeLookup: CountryCodeLookup) {


  val OrganizationType = "Partnership"

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def iformXmlToSubmission(xml: generated.partnership.Data, brd: BusinessRegistrationDetails): SubScriptionCreate = {
    SubScriptionCreate(
      requestType = "Create",
      SubscriptionCreateRequestSchema(
        organizationType = brd.businessType.map(translateBusinessType).getOrElse(OrganizationType),
        FHbusinessDetail = IsNewFulfilmentBusiness(
          isNewFulfilmentBusiness = isYes(xml.isNewFulfilmentBusiness.isNewFulfilmentBusiness),
          proposedStartDate = getProposedStartDate(xml)),
        GroupInformation = None,
        additionalBusinessInformation = additionalBusinessInformation(brd, xml),
        businessDetail = businessDetails(xml, brd),
        businessAddressForFHDDS = businessAddressForFHDDS(xml, brd),
        contactDetail = contactDetail(xml, brd),
        declaration = declaration(xml.declaration)
      ))
  }

  def addressLookupToAddress(addressLookup: partnership.AddressLookUpContactAddress): Option[Address] = {
    if (isYes(addressLookup.selectLocation)) {
      addressLookup.ukPanel.flatMap(_.blockAddressUKPlus).map(ukAddressToAddress)
    } else {
      addressLookup.blockAddressInternationalPlus.map(internationalAddressToAddress)
    }
  }

  def businessDetails(xml: generated.partnership.Data, brd: BusinessRegistrationDetails): BusinessDetail = {
    //TODO: find the correct
    BusinessDetail(
      soleProprietor = None,
      nonProprietor = Some(NonProprietor(
        tradingName = whenYes(
          xml.tradingName.hasTradingName,
          xml.tradingName.panelTradingName.map(_.tradingName)),
        identification = NonProprietorIdentification(
          uniqueTaxpayerReference = brd.utr,
          vatRegistrationNumber = whenYes(
            xml.vatRegistration.hasVatRegistrationNumber,
            xml.vatRegistration.panelVatRegistrationNumber.map(_.vatRegistrationNumber))
        )
      )),
      limitedLiabilityPartnershipCorporateBody = None,
      partnership = Some(partnership1(xml.partners))
    )
  }
  def partnership1(panel: partnership.Partners): Partnership = {
    val partnerDetails = panel.panelRepeatingPartner.toList map {
      repeatingPanel ⇒ PartnerDetail(
        repeatingPanel.entityType,
        partnerAddress = repeatingPanel
          .partnerAddress
          .flatMap(_.ukPanel)
          .flatMap(_.block_addressUKPlus)
          .map(ukAddressToAddress)
          .get,
        partnerTypeDetail = PartnershipUtils partner repeatingPanel,
        modification = None
      )
    }
    Partnership(
      numbersOfPartners = partnerDetails.size.toString,
      partnerDetails
    )
  }

  def businessAddressForFHDDS(xml: generated.partnership.Data, brd: BusinessRegistrationDetails): BusinessAddressForFHDDS = {
    val lessThan3Years: Boolean =
      xml.timeAtCurrentAddress.timeOperatedAtCurrentAddress == "Less than 3 years"

    BusinessAddressForFHDDS(
      currentAddress = principalBusinessAddress(brd),
      commonDetails = CommonDetails(),
      timeOperatedAtCurrentAddress = xml.timeAtCurrentAddress.timeOperatedAtCurrentAddress,
      previousOperationalAddress = {if (!lessThan3Years) None
        else {
          if (isYes(xml.timeAtCurrentAddress.panelAnyPreviousOperatingAddress.get.anyPreviousOperatingAddress)) {
            val previousOperationalAddressDetails =
              PreviousOperationalAddressDetail(
                previousAddress = {
                  val address = xml.timeAtCurrentAddress.panelAnyPreviousOperatingAddress.get.panelPreviousAddress.get.ukPanel.get.block_addressUKPlus.get
                  ukAddressToAddress(address)
                },
                previousAddressStartdate = LocalDate.parse(xml.timeAtCurrentAddress.panelAnyPreviousOperatingAddress.get.panelPreviousAddress.get.operatingDate.get, dtf))
            Some(PreviousOperationalAddress(
              true,
              Some(List(previousOperationalAddressDetails))
            ))
          } else {
            Some(PreviousOperationalAddress(
              false,
              None
            ))
          }
        }
      }
    )
  }

  def principalBusinessAddress(brd: BusinessRegistrationDetails): Address = {
    Address(line1 = brd.businessAddress.line1,
      line2 = brd.businessAddress.line2.noneIfBlank,
      line3 = brd.businessAddress.line3.noneIfBlank,
      line4 = brd.businessAddress.line4,
      postalCode = brd.businessAddress.postcode,
      countryCode = brd.businessAddress.country)
  }

  def ukAddressToAddress(blockAddressUk: partnership.AddressUKPlusQuestion) =
    Address(
      blockAddressUk.line1,
      blockAddressUk.line2.noneIfBlank,
      blockAddressUk.line3.noneIfBlank,
      blockAddressUk.town,
      Some(blockAddressUk.postcode),
      "GB")

  def internationalAddressToAddress(blockAddressInternationalPlus: partnership.AddressInternationalPlusQuestion) = {
    val countryCode = countryCodeLookup.countryCode(blockAddressInternationalPlus.country)
    Address(
      blockAddressInternationalPlus.line1,
      blockAddressInternationalPlus.line2.noneIfBlank,
      blockAddressInternationalPlus.line3.noneIfBlank,
      blockAddressInternationalPlus.country.noneIfBlank,
      None,
      countryCode.get)
  }

  private def translateBusinessType(businessType: String) = WordUtils.capitalizeFully(businessType) match {
    case "Sole Trader" ⇒ "Sole Proprietor"
    case other         ⇒ other
  }

  private def getProposedStartDate(xml: partnership.Data) = {
    for {
      panelProposedStartDate <- xml.isNewFulfilmentBusiness.panelProposedStartDate
    } yield {
      LocalDate.parse(panelProposedStartDate.proposedStartDate, dtf)
    }
  }

  private def declaration(declaration: generated.partnership.Declaration) = {
    Declaration(personName = declaration.hide_personName,
      personStatus = declaration.hide_personStatus,
      email = declaration.hide_Email,
      isInformationAccurate = true)
  }

  private def additionalBusinessInformation(brd: BusinessRegistrationDetails, xml: partnership.Data) = {
    val numberOfCustomersData = xml.numberOfCustomers.numberOfCustomers
    val isVatReg = isYes(xml.vatRegistration.hasVatRegistrationNumber)
    val eoriStatus = xml.eoriStatus

    val otherStorageSitesDetail = {
      if (isYes(xml.otherStorageSites.hasOtherStorageSites)) {
        otherStorageSitesDetails(xml)
      } else List()
    }

    AdditionalBusinessInformationwithType(
      partnerCorporateBody = None,
      allOtherInformation = AllOtherInformation(
        numberOfCustomers = numberOfCustomersData,
        doesEORIExist = isYes(eoriStatus.doesEORIExist),
        EORINumber = eoriStatus.EORINumber map eoriNumber(isVatReg),
        numberOfpremises = otherStorageSitesDetail.length.toString,
        premises = otherStorageSitesDetail
      )
    )
  }

  def eoriNumber(isVatReg: Boolean)(eori: generated.partnership.EORINumber) = {
    val eoriNumberGoodsImportedOutEORI = isYes(eori.goodsImportedOutEORI)
    if (isVatReg) {
      EORINumberType(
        EORIVat = eori.EORI,
        EORINonVat =  None,
        goodsImportedOutEORI = Some(eoriNumberGoodsImportedOutEORI))
    } else {
      EORINumberType(
        EORIVat = None,
        EORINonVat = eori.EORI,
        goodsImportedOutEORI = Some(eoriNumberGoodsImportedOutEORI))
    }
  }

  private def otherStorageSitesDetails(xml: partnership.Data) = {
    for {
      otherStorageSites ← xml.otherStorageSites.panelOtherStorageSites.toList
      blockAddressUKPlus ← otherStorageSites.repeatingSectionOtherTradingPremises
      repeatingPanel ← blockAddressUKPlus.repeatingPanel
      thirdPartyPremises = repeatingPanel.thirdPartyPremises
      addressLookup ← repeatingPanel.otherTradingPremisesAddressLookup.ukPanel
      blockAddressUKPlus ← addressLookup.blockAddressUKPlus
    } yield {
      Premises(
        address = ukAddressToAddress(blockAddressUKPlus),
        thirdPartyPremises = isYes(thirdPartyPremises),
        //todo set modification for amend
        modification = None
      )
    }
  }

  private def contactDetail(xml: partnership.Data, brd: BusinessRegistrationDetails) = {
    val contactPersonAddress = extractContactPersonAddress(xml, brd)

    ContactDetail(
      title = None,
      names = Name(
        firstName = xml.contactPerson.firstName,
        middleName = None,
        lastName = xml.contactPerson.lastName),
      usingSameContactAddress = isYes(xml.contactPerson.contactCorrectAddress),
      address = contactPersonAddress,
      commonDetails = CommonDetails(
        telephone = Some(xml.contactPerson.telephoneNumber),
        mobileNumber = None,
        email = Some(xml.contactPerson.email)),
      roleInOrganization = Some(RoleInOrganization otherRole xml.contactPerson.otherRoleDescription)
    )
  }

  private def extractContactPersonAddress(xml: partnership.Data, brd: BusinessRegistrationDetails): Option[Address] = {
    if (isYes(xml.contactPerson.contactCorrectAddress)) {
      Some(principalBusinessAddress(brd))
    } else {
      xml.contactPerson.addressLookUpContactAddress.flatMap(addressLookupToAddress)
    }
  }


  private def checkEmptyAddressLine(addressLine: String): String = {
    if (addressLine.isEmpty) " "
    else addressLine
  }
}
