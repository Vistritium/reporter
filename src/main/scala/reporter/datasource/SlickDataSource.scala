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
      logger.info(s"Result row: ${resultRow}")
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
      case Types.BIT => StringType(if (positionedResult.nextInt() == 0) "false" else "true")
      case Types.TINYINT => NumberType(positionedResult.nextInt())
      case Types.SMALLINT => NumberType(positionedResult.nextInt())
      case Types.INTEGER => NumberType(positionedResult.nextInt())
      case Types.BIGINT => NumberType(positionedResult.nextLong())
      case Types.FLOAT => NumberType(positionedResult.nextDouble())
      case Types.REAL => NumberType(positionedResult.nextDouble())
      case Types.DOUBLE => NumberType(positionedResult.nextDouble())
      case Types.NUMERIC => NumberType(positionedResult.nextDouble())
      case Types.DECIMAL => NumberType(positionedResult.nextDouble())
      case Types.DATE => DateType(positionedResult.nextDate().toInstant)
      case Types.TIMESTAMP => LocalDateTimeType(positionedResult.nextTimestamp().toLocalDateTime)
      case Types.TIME => LocalTimeType(positionedResult.nextTime().toLocalTime)
      case _ => StringType(positionedResult.nextString())
    }


  }

}