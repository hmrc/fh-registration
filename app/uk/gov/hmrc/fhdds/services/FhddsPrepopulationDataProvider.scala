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

import play.api.libs.json.Json
import uk.gov.hmrc.fhdds.connectors.{CompaniesHouseConnector, DESConnector}
import uk.gov.hmrc.fhdds.models._
import uk.gov.hmrc.fhdds.models.businessMatching.{EtmpAddress, FindBusinessDataResponse, OrganisationResponse}
import uk.gov.hmrc.fhdds.models.companyHouse.ChAddress
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FhddsPrepopulationDataProvider {
  val businessContactDetailsField = "/data/businessContactDetails/address"
  val principalAddressField = "/data/principalPlace/address"
  val directorFirstNameField = "/data/directors/firstName"
  val directorLastNameField = "/data/directors/lastName"
  val businessContactField = "/data/businessContact/address"
  val businessPremisesField = "/data/businessPremises/address"

  def getCompanyDetails(utr: String): Future[CompanyDetails] = {
    DESConnector.lookup(utr).map(parseDesResponse).flatMap {
      case None                 ⇒ Future successful CompanyDetails(None, None)
      case Some((address, org)) ⇒ getCompanyFromCH(org.organisationName).flatMap {
        case None          ⇒ Future successful CompanyDetails(Some(convertAddress(address)), Some(companyFromDes(org)))
        case Some(company) ⇒ getDetailsFromCh(company)
      }
    }
  }

  def companyFromDes(org: OrganisationResponse) = Company(org.organisationName)

  def convertAddress(address: EtmpAddress) = Address(
    address.postalCode.getOrElse(""),
    address.addressLine1,
    address.addressLine2,
    address.addressLine3,
    address.addressLine4,
    None,
    address.countryCode
  )

  def getDetailsFromCh(company: companyHouse.Company): Future[CompanyDetails] = {
    val address = Address(
      company.address.postalCode,
      company.address.addressLine1,
      company.address.addressLine2,
      company.address.addressLine3,
      company.address.addressLine4,
      company.address.region,
      company.address.country
    )

    getOfficer(company)
      .recover {case _ ⇒ None}
      .map { officer ⇒
        val c = Company(
          company.title,
          Option(company.companyNumber),
          Option(company.companyType),
          Option(company.companyStatus),
          officer
        )
        CompanyDetails(Some(address), Some(c))
      }
    }

  def getOfficer(company: companyHouse.Company): Future[Option[Officer]] = {
    CompaniesHouseConnector.registeredOfficers(company.companyNumber) map findOneDirector
  }

  def findOneDirector(officers: List[companyHouse.Officer]): Option[Officer] = {
    officers
      .find { officer ⇒ officer.resignedOn.isEmpty && officer.officerRole.equals("director") }
      .map { chOfficer ⇒
        Officer(
          chOfficer.firstName,
          chOfficer.lastName,
          chOfficer.resignedOn,
          chOfficer.officerRole
        )
      }
  }

  def formatChAddress(address: ChAddress) = {
    address.premises.map(_ + "</br>").getOrElse("") +
      address.addressLine1.map(_ + "</br>").getOrElse("") +
      address.addressLine2.map(_ + "</br>").getOrElse("") +
      address.addressLine3.map(_ + "</br>").getOrElse("") +
      address.addressLine4.map(_ + "</br>").getOrElse("") +
      address.locality.map(_ + "</br>").getOrElse("") +
      address.country.map(_ + "</br>").getOrElse("") +
      address.postalCode + "</br>"
  }

  def getCompanyFromCH(title: String): Future[Option[companyHouse.Company]] = {
    CompaniesHouseConnector
      .fuzzySearchCompany(title)
      .map { companies ⇒ companies.find(_.title.equalsIgnoreCase(title)) }
  }

  def parseDesResponse(response: HttpResponse): Option[(EtmpAddress, OrganisationResponse)] = {
    for {
      businessData ← Json.parse(response.body).validate[FindBusinessDataResponse].asOpt
      address = businessData.address
      organization ← businessData.organisation
    } yield (address, organization)
  }

  def formatDesAddress(etmpAddress: EtmpAddress) = {
    etmpAddress.addressLine1.map(_ + "</br>").getOrElse("") +
      etmpAddress.addressLine2.map(_ + "</br>").getOrElse("") +
      etmpAddress.addressLine3.map(_ + "</br>").getOrElse("") +
      etmpAddress.addressLine4.map(_ + "</br>").getOrElse("") +
      etmpAddress.postalCode.map(_ + "</br>").getOrElse("") +
      etmpAddress.countryCode.map(_ + "</br>").getOrElse("")
  }
}
