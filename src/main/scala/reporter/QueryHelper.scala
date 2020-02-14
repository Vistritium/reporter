package reporter

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, Instant, LocalDate}

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils

case class QueryContainer(
  query: String
)

case class QueryMeta(
  databaseName: String,
  queryTemplate: String,
  params: List[String]
) {

  def evaluate(paramsWithValues: Map[String, String], queryHelper: QueryHelper): String = {
    require(paramsWithValues.keySet == this.params.toSet, s"${this} - incorrect provided params: $paramsWithValues")
    queryHelper.evaluate(queryTemplate, paramsWithValues)
  }

}

@Singleton
class QueryHelper @Inject()(
) extends LazyLogging {

  val Pattern = """\$\{(.*?)\}""".r

  def info(database: String, query: String): QueryMeta = {

    val params = findParams(query)

    QueryMeta(database, query, params)
  }

  private def findParams(query: String) = {
    Pattern.findAllIn(query).toList
      .map(s => StringUtils.substringBetween(s, "${", "}"))
  }

  def evaluate(query: String, paramValues: Map[String, String]): String = {
    val params = findParams(query)
    val queryWithParams = params.foldLeft(query) { case (query, param) =>
      val replacement = paramValues(param)
      query.replace("${" + param + "}", s"'${replacement}'")
    }
    val queryWithParamsAndComputedValues =
      computedValues.foldLeft(queryWithParams) { case (query, computedValue) =>
        val templatedKey = "$[" + computedValue.key + "]"
        if (query.contains(templatedKey)) {
          query.replace(templatedKey, s"'${computedValue.replacement()}'")
        } else query
      }
    queryWithParamsAndComputedValues
  }


  trait ComputedValue {
    val key: String
    def replacement(): String

  }

  val LocalDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val computedValues = List(
    new ComputedValue {
      override val key: String = "BEGINNING_CURRENT_MONTH"

      override def replacement(): String = {
        val now = LocalDate.now()
        LocalDate.of(now.getYear, now.getMonth, 0)
          .format(LocalDateFormat)
      }
    },
    new ComputedValue {
      override val key: String = "BEGINNING_NEXT_MONTH"

      override def replacement(): String = {
        val now = LocalDate.now()
        LocalDate.of(now.getYear, now.getMonth, 0).plusMonths(1)
          .format(LocalDateFormat)
      }
    },
    new ComputedValue {
      override val key: String = "BEGINNING_CURRENT_WEEK"

      override def replacement(): String = {
        val now = LocalDate.now()

        def iter(dt: LocalDate): LocalDate = {
          if (now.getDayOfWeek == DayOfWeek.MONDAY) dt
          else iter(dt.minusDays(1))
        }

        iter(LocalDate.now())
          .format(LocalDateFormat)
      }
    },
    new ComputedValue {
      override val key: String = "BEGINNING_LAST_WEEK"

      override def replacement(): String = {
        val now = LocalDate.now()

        def iter(dt: LocalDate): LocalDate = {
          if (now.getDayOfWeek == DayOfWeek.MONDAY) dt
          else iter(dt.minusDays(1))
        }

        iter(LocalDate.now().minusDays(7))
          .format(LocalDateFormat)
      }
    }

    ,
  )


}