package reporter

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.FileIO
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import reporter.datasource.SlickDataSourceFactory
import reporter.excel.{ExcelManager, ExcelProcessor}

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class Starter @Inject()(
  config: Config,
  excelProcessor: ExcelProcessor,
  slickDataSourceFactory: SlickDataSourceFactory,
  querySource: QueryHelper,
  private implicit val actorSystem: ActorSystem,
  excelManager: ExcelManager
) extends LazyLogging {
  def start(): Unit = {

    val meta = excelManager.excels.head.metadata

    val source = excelManager.excels.head.generate(Map.empty)
    Await.result(source.runWith(FileIO.toPath(Paths.get("populated-budget.xlsx"))), 20.seconds)
    logger.info("Done")

  }
}
