package reporter.datasource

import java.time.{Instant, LocalDateTime, LocalTime}

import akka.NotUsed
import akka.stream.scaladsl.Source

import scala.concurrent.Future

trait SourceContainer extends AutoCloseable {
  val headers: Future[List[String]]
  val source: Source[ResultRow, NotUsed]
}

case class ResultRow(
  fields: Seq[ExcelSupportedType]
)

sealed abstract class ExcelSupportedType
case class StringType(s: String) extends ExcelSupportedType
case class DateType(i: Instant) extends ExcelSupportedType
case class LocalDateTimeType(i: LocalDateTime) extends ExcelSupportedType
case class LocalTimeType(i: LocalTime) extends ExcelSupportedType
case class LocalDate(i: java.time.LocalDate) extends ExcelSupportedType
case class NumberType(d: Option[Double]) extends ExcelSupportedType

trait DataSource {
  def source: SourceContainer
}
