package reporter.datasource

import java.sql.{ResultSet, Types}

import akka.NotUsed
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import com.google.inject.assistedinject.Assisted
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.{GetResult, PositionedResult}

import scala.concurrent.{Future, Promise}

class SlickDataSource @Inject()(
  @Assisted("dbConfigPath") dbConfigPath: String,
  @Assisted("query") query: String
) extends DataSource with LazyLogging {
  override def source: SourceContainer = {

    val headersPromise = Promise.apply[List[String]]()

    val fullDbConfigPath = s"databases.$dbConfigPath"
    implicit val session: SlickSession = SlickSession.forConfig(fullDbConfigPath)
    import session.profile.api._
    implicit val getResult: GetResult[ResultRow] = GetResult(r => {

      if (!headersPromise.isCompleted) {
        headersPromise.success(
          (1 to r.numColumns).map { col =>
            r.rs.getMetaData.getColumnName(col)
          }.toList
        )
      }
      val types = (1 to r.numColumns).map { col =>
        col -> r.rs.getMetaData.getColumnType(col)
      }.toMap

      val resultRow = ResultRow((1 to r.numColumns).map { i =>
        getType(r, types(i))
      })
      logger.debug(s"Result row: ${resultRow}")
      resultRow
    })

    val resultSource = Slick.source(sql"#$query".as[ResultRow])

    new SourceContainer {
      override val headers: Future[List[String]] = headersPromise.future
      override val source: Source[ResultRow, NotUsed] = resultSource

      override def close(): Unit = session.close()
    }

  }

  def getType(positionedResult: PositionedResult, columnType: Int): ExcelSupportedType = {
    columnType match {
      case Types.BIT => StringType(positionedResult.nextIntOption().map(s => if (s == 0) "false" else "true").orNull)
      case Types.TINYINT => NumberType(positionedResult.nextIntOption().map(_.toDouble))
      case Types.SMALLINT => NumberType(positionedResult.nextIntOption().map(_.toDouble))
      case Types.INTEGER => NumberType(positionedResult.nextIntOption().map(_.toDouble))
      case Types.BIGINT => NumberType(positionedResult.nextLongOption().map(_.toDouble))
      case Types.FLOAT => NumberType(positionedResult.nextDoubleOption())
      case Types.REAL => NumberType(positionedResult.nextDoubleOption())
      case Types.DOUBLE => NumberType(positionedResult.nextDoubleOption())
      case Types.NUMERIC => NumberType(positionedResult.nextDoubleOption())
      case Types.DECIMAL => NumberType(positionedResult.nextDoubleOption())
      case Types.DATE => LocalDate(Option(positionedResult.nextDate()).map(_.toLocalDate).orNull)
      case Types.TIMESTAMP => LocalDateTimeType(Option(positionedResult.nextTimestamp()).map(_.toLocalDateTime).orNull)
      case Types.TIME => LocalTimeType(Option(positionedResult.nextTime()).map(_.toLocalTime).orNull)
      case _ => StringType(positionedResult.nextString())
    }


  }

}