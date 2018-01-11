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

package uk.gov.hmrc.fhregistration.services

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import generated.limited.{AddressInternationalPlusQuestion, AddressLookUpContactAddress, AddressUKPlusQuestion, Data}
import org.apache.commons.lang3.text.WordUtils
import uk.gov.hmrc.fhregistration.models.businessregistration.BusinessRegistrationDetails
import uk.gov.hmrc.fhregistration.models.des._
import uk.gov.hmrc.fhregistration.services.ApplicationUtils._

@Singleton
class FhddsApplicationServiceImpl @Inject()(val countryCodeLookup: CountryCodeLookup)
  extends FhddsApplicationService

@ImplementedBy(classOf[FhddsApplicationServiceImpl])
trait FhddsApplicationService {

  val countryCodeLookup: CountryCodeLookup
  val DefaultOrganizationType = "Corporate Body"

  val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  def iformXmlToApplication(xml: generated.limited.Data, brd: BusinessRegistrationDetails): SubScriptionCreate = {
    SubScriptionCreate(
      requestType = "Create",
      SubscriptionCreateRequestSchema(
      organizationType = brd.businessType.map(translateBusinessType).getOrElse(DefaultOrganizationType),
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

  def companyOfficialsDetails(xml: generated.limited.Data): List[CompanyOfficial] = {
    val companyOfficials = xml.companyOfficials.panelRepeatingCompanyOfficial
    companyOfficials.toList.map(
      companyOfficial ⇒
        companyOfficial.companyOfficerType match {
          case "Individual" ⇒ getCompanyOfficialAsPerson(companyOfficial.panelPerson.get)
          case "Company" ⇒ getCompanyOfficialAsCompany(companyOfficial.panelCompany.get)
        }
    )
  }

  def addressLookupToAddress(addressLookup: AddressLookUpContactAddress): Option[Address] = {
    if (isYes(addressLookup.selectLocation)) {
      addressLookup.ukPanel.flatMap(_.blockAddressUKPlus).map(ukAddressToAddress)
    } else {
      addressLookup.blockAddressInternationalPlus.map(internationalAddressToAddress)
    }
  }

  def businessDetails(xml: generated.limited.Data, brd: BusinessRegistrationDetails): BusinessDetail = {
    BusinessDetail(
      soleProprietor = None,
      nonProprietor = Some(NonProprietor(
        tradingName =
          if (isYes(xml.tradingName.hasTradingName))
            xml.tradingName.panelTradingName.map(_.tradingName)
          else
            None,
        identification = NonProprietorIdentification(
          uniqueTaxpayerReference = brd.utr,
          vatRegistrationNumber =
            if (isYes(xml.vatRegistration.hasVatRegistrationNumber))
              xml.vatRegistration.panelVatRegistrationNumber.map(_.vatRegistrationNumber)
            else
              None
        )
      )),
      Some(LimitedLiabilityPartnershipCorporateBody(
        groupRepresentativeJoinDate = None,
        incorporationDetails = IncorporationDetails(
          companyRegistrationNumber = Some(
            xml.businessDetail.limitedLiabilityPartnershipCorporateBody.companyRegistrationNumber),
          dateOfIncorporation = Some(
            LocalDate.parse(xml.dateOfIncorporation.dateOfIncorporation, dtf))
        )
      )),
      partnership = None
    )
  }

  def businessAddressForFHDDS(xml: generated.limited.Data, brd: BusinessRegistrationDetails): BusinessAddressForFHDDS = {
    val lessThan3Years: Boolean =
      xml.timeAtCurrentAddress.timeOperatedAtCurrentAddress == "Less than 3 years"

    BusinessAddressForFHDDS(
      currentAddress = principalBusinessAddress(brd),
      commonDetails = CommonDetails(),
      timeOperatedAtCurrentAddress = xml.timeAtCurrentAddress.timeOperatedAtCurrentAddress,
      previousOperationalAddress = {
        if (!lessThan3Years) None
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

//  def previousPrincipalPlaceOfBusinessAddresses(previousAddress: PanelPreviousAddress): Option[PreviousOperationalAddress] = {
//    for {
//      panelPreviousPrincipalTradingBusinessAddresses ← previousAddress.ukPanel
//      blockAddressUKPlus ← panelPreviousPrincipalTradingBusinessAddresses.block_addressUKPlus
//      operatingDate = previousAddress.operatingDate
//    } yield {
//      PreviousOperationalAddress(
//        operatingDate = LocalDate.parse(operatingDate, dtf),
//        previousAddress = Address(
//          blockAddressUKPlus.line1,
//          blockAddressUKPlus.line2.nonEmptyString,
//          blockAddressUKPlus.line3.noneIfBlank,
//          blockAddressUKPlus.town,
//          Some(blockAddressUKPlus.postcode),
//          "GB"))
//    }
//  }

  def principalBusinessAddress(brd: BusinessRegistrationDetails): Address = {
    Address(line1 = brd.businessAddress.line1,
      line2 = brd.businessAddress.line2.noneIfBlank,
      line3 = brd.businessAddress.line3.noneIfBlank,
      line4 = brd.businessAddress.line4,
      postalCode = brd.businessAddress.postcode,
      countryCode = brd.businessAddress.country)
  }

  def ukAddressToAddress(blockAddressUk: AddressUKPlusQuestion) =
    Address(
      blockAddressUk.line1,
      blockAddressUk.line2.noneIfBlank,
      blockAddressUk.line3.noneIfBlank,
      blockAddressUk.town,
      Some(blockAddressUk.postcode),
      "GB")

  def internationalAddressToAddress(blockAddressInternationalPlus: AddressInternationalPlusQuestion) = {
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

  private def getProposedStartDate(xml: Data) = {
    for {
      panelProposedStartDate <- xml.isNewFulfilmentBusiness.panelProposedStartDate
    } yield {
      LocalDate.parse(panelProposedStartDate.proposedStartDate, dtf)
    }
  }

  private def declaration(declaration: generated.limited.Declaration) = {
    Declaration(personName = declaration.hide_personName,
      personStatus = declaration.hide_personStatus,
      email = email(declaration),
      isInformationAccurate = true)
  }

  private def email(declaration: generated.limited.Declaration): Option[String] = {
    if (declaration.panelHasGGEmail.map(_.hide_GGEmail).getOrElse("").isEmpty) {
      //NO gg email
      declaration.panelNoGGEmail.map(_.hide_confirmNewEmail)
    } else {
      //HAS gg email
      declaration.panelHasGGEmail flatMap { ggEmailPanel ⇒
        if (isYes(ggEmailPanel.hide_useGGEmail))
          Some(ggEmailPanel.hide_GGEmail)
        else {
          ggEmailPanel.panelAlternateEmail.map(_.hide_confirmationEmail)
        }
      }
    }

  }

  private def additionalBusinessInformation(brd: BusinessRegistrationDetails, xml: Data) = {
    //val numberOfCustomersOutsideOfEU = xml.businessActivities.numberOfCustomersOutsideOfEU
    val numberOfCustomersData = xml.numberOfCustomers.numberOfCustomers
    val isVatReg = isYes(xml.vatRegistration.hasVatRegistrationNumber)
    val eoriStatus = xml.eoriStatus

    val officials = companyOfficialsDetails(xml)

    // TODO should we always add the principalBusinessAddress(brd) at the beginning of the list?
    val otherStorageSitesDetail = {
      if (isYes(xml.otherStorageSites.hasOtherStorageSites)) {
        otherStorageSitesDetails(xml)
      } else List(Premises(
        address = principalBusinessAddress(brd),
        thirdPartyPremises = false,
        //todo set modification for amend
        modification = None)
      )
    }

    AdditionalBusinessInformationwithType(
      partnerCorporateBody = Some(
        PartnerCorporateBody(
          numberOfOtherOfficials = officials.size.toString,
          companyOfficials = Some(officials)
        )
      ),
      allOtherInformation = AllOtherInformation(
        numberOfCustomers = numberOfCustomersData,
        doesEORIExist = isYes(eoriStatus.doesEORIExist),
        EORINumber = eoriStatus.EORINumber map eoriNumber(isVatReg),
        numberOfpremises = otherStorageSitesDetail.length.toString,
        premises = otherStorageSitesDetail
      )
    )
  }

  def eoriNumber(isVatReg: Boolean)(eori: generated.limited.EORINumber) = {
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

  private def otherStorageSitesDetails(xml: Data) = {
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

  private def contactDetail(xml: Data, brd: BusinessRegistrationDetails) = {
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
      roleInOrganization = None
    )
  }

  private def extractContactPersonAddress(xml: Data, brd: BusinessRegistrationDetails): Option[Address] = {
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
