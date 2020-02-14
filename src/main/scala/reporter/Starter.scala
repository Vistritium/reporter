package reporter

import java.nio.file.Paths

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import reporter.datasource.SlickDataSourceFactory
import reporter.excel.ExcelManager

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

@Singleton
class Starter @Inject()(
  config: Config,
  slickDataSourceFactory: SlickDataSourceFactory,
  querySource: QueryHelper,
  implicit val system: ActorSystem[AppSupervisor.Command],
  implicit val ec: ExecutionContext
) extends LazyLogging {
  def start(): Unit = {

  }
}
