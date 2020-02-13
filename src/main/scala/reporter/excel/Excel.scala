package reporter.excel

import java.nio.file.{Files, Path}
import java.util.Date

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import com.google.inject.assistedinject.Assisted
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import org.apache.poi.ss.SpreadsheetVersion
import org.apache.poi.ss.usermodel.CellCopyPolicy
import org.apache.poi.ss.util.{AreaReference, CellAddress, CellReference}
import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFRow, XSSFTableColumn, XSSFWorkbook}
import reporter.datasource.{DateType, LocalDateTimeType, LocalTimeType, NumberType, SlickDataSourceFactory, StringType}
import reporter.{QueryHelper, QueryMeta}

import scala.concurrent.duration._
import collection.JavaConverters._
import resource._

import scala.concurrent.{Await, ExecutionContext}
import scala.io.{Source => ScalaIoSource}


class Excel @Inject()(
  @Assisted path: Path,
  @Assisted val name: String,
  private implicit val executionContext: ExecutionContext,
  slickDataSourceFactory: SlickDataSourceFactory,
  queryHelper: QueryHelper,
  private implicit val actorSystem: ActorSystem,
) extends LazyLogging {

  private val CopyPolicy = new CellCopyPolicy.Builder().build()

  case class TableMeta(
    tableName: String,
    sheetName: String,
    query: QueryMeta
  )

  case class Metadata(
    tables: Seq[TableMeta]
  )

  val metadata = {
    managed(new XSSFWorkbook(path.toFile)).acquireAndGet { wb =>
      Metadata(
        wb.sheetIterator().asScala
          .flatMap { sheet => Option(wb.getSheet(sheet.getSheetName)) }
          .flatMap { sheet => sheet.getTables.asScala }
          .flatMap { table =>
            Option(table.getXSSFSheet.getCellComment(new CellAddress(table.getStartCellReference)))
              .filter(_.getString.getString.startsWith("template"))
              .map(table -> _)
          }.map { case (table, comment) =>
          val database :: queryLines = ScalaIoSource.fromString(comment.getString.getString).getLines().drop(1).toList
          val query = queryLines.mkString("")
          logger.info(s"Database ${database} table ${table.getName} query ${query}")
          val info = queryHelper.info(database, query)
          TableMeta(
            table.getName,
            table.getSheetName,
            info
          )
        }.toList
      )
    }
  }


  def generate(params: Map[String, String]): Source[ByteString, NotUsed] = {
    Source.single()
      .flatMapConcat { _ =>
        val wb = new XSSFWorkbook(path.toFile)
        val tmpSheet = wb.createSheet("______tmp_______")

        Source(metadata.tables.toList).zipWithIndex.mapAsync(1) { case (metaTable, tableI) =>
          val query = metaTable.query.query(params)

          val source = slickDataSourceFactory.get(metaTable.query.databaseName, query).source //TODO not closed

          val sheet = wb.getSheet(metaTable.sheetName)
          val table = wb.getTable(metaTable.tableName)
          val tmpRow = tmpSheet.createRow(tableI.toInt)
          val firstCell = table.getArea.getFirstCell

          def getRowHandle(i: Int): XSSFRow = sheet.getRow(i) match {
            case null => sheet.createRow(i)
            case row => row
          }

          def getRow(r: Int): Int = firstCell.getRow + r + 1

          def getCol(c: Int): Int = firstCell.getCol + c

          lazy val columns: List[ColumnType] = {
            val header = Await.result(source.headers, 3.seconds)
            val headerToPosition = header.zipWithIndex.toMap
            Await.result(source.headers, 3.seconds)
            table.getColumns.asScala.map { case column =>
              val cell = sheet.getRow(getRow(0)).getCell(getCol(column.getColumnIndex))
              val tmpCell = tmpRow.createCell(getCol(column.getColumnIndex))
              tmpCell.copyCellFrom(cell, CopyPolicy)
              if (cell.getCellComment != null) {
                FillColumn(headerToPosition
                  .getOrElse(
                    column.getName,
                    throw new IllegalStateException(s"Could not map column ${column.getName} in returned sql data: ${header.mkString(", ")} ")
                  ), column, tmpCell)
              } else {
                CopyColumn(column, tmpCell)
              }
            }.toList
          }

          source.source.zipWithIndex.fold(0) { case (count, (resultRow, rowI)) =>
            val row = getRowHandle(getRow(rowI.toInt))
            columns.foreach { column =>
              val cell = row.createCell(getCol(column.column.getColumnIndex))
              column match {
                case FillColumn(order, column, tmpCell) => {
                  val value = resultRow.fields(order)
                  cell.copyCellFrom(tmpCell, CopyPolicy)
                  logger.debug(s"Setting cell ${cell.getAddress} sheet ${sheet.getSheetName} value ${value}")
                  value match {
                    case StringType(s) => cell.setCellValue(s)
                    case DateType(i) => cell.setCellValue(Date.from(i))
                    case LocalDateTimeType(i) => cell.setCellValue(i)
                    case LocalTimeType(i) => cell.setCellValue(i.toString)
                    case NumberType(d) => cell.setCellValue(d)
                  }
                }
                case CopyColumn(column, tmpCell) => {
                  cell.copyCellFrom(tmpCell, CopyPolicy)
                }
              }
            }
            count + 1
          }.map { count =>
            table.setArea(new AreaReference(
              table.getArea.getFirstCell,
              new CellReference(getRow(count) - 1, getCol(table.getColumnCount) - 1),
              SpreadsheetVersion.EXCEL2007
            ))
          }.runWith(Sink.ignore)
        }.map(_ => wb)
      }.reduce((wb, _) => wb)
      .flatMapConcat { wb =>
        // wb.removeSheetAt(wb.getSheetIndex(tmpSheet))
        wb.setForceFormulaRecalculation(true)
        val targetFile = Files.createTempFile(s"reporter_dst_${path.getFileName}", ".xlsx")
        managed(Files.newOutputStream(targetFile)).foreach { os =>
          wb.write(os)
        }

        FileIO.fromPath(targetFile)
          .watchTermination()((f, _) => f.map(r => {
            logger.info(s"Stream closed, removing file ${targetFile}")
            Files.delete(targetFile)
          }))
      }
  }

  private sealed abstract class ColumnType(val column: XSSFTableColumn, val tmpCell: XSSFCell)

  private case class FillColumn(order: Int, override val column: XSSFTableColumn, override val tmpCell: XSSFCell) extends ColumnType(column, tmpCell)

  private case class CopyColumn(override val column: XSSFTableColumn, override val tmpCell: XSSFCell) extends ColumnType(column, tmpCell)


}


