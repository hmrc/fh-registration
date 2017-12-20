package uk.gov.hmrc.fhdds.models.des

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json._


case class Modification(changeIndicator: String,
                        changeDate: Option[LocalDate])

object Modification {
  val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  implicit val localDateReads = Reads.localDateReads("yyyy-MM-dd")
  implicit val localDateWrites = Writes { date: LocalDate ⇒
    JsString(date.format(dateTimeFormatter))
  }
  implicit val format = Json.format[IndividualIdentification]
}