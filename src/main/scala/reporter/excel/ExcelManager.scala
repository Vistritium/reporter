package reporter.excel

import java.io.ByteArrayInputStream
import java.nio.file.{FileVisitOption, Files, Path, StandardCopyOption}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.util.ByteString
import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.zeroturnaround.zip.ZipUtil
import resource.managed

import collection.JavaConverters._

object ExcelManager {

  sealed trait Command

  final case class ExcelsZip(data: ByteString) extends Command

  final case class ReadExcels(replyTo: ActorRef[RespondExcels]) extends Command

  final case class RespondExcels(excels: List[Excel])

}

@Singleton
class ExcelManager @Inject()(
  context: ActorContext[ExcelManager.Command],
  excelFactory: ExcelFactory
) extends AbstractBehavior[ExcelManager.Command](context) {

  var excels: List[Excel] = List.empty
  var currentDirectory: Option[Path] = None

  override def onMessage(msg: ExcelManager.Command): Behavior[ExcelManager.Command] = msg match {
    case ExcelManager.ExcelsZip(data) => {

      val tmpDir = Files.createTempDirectory("reporter")
      context.log.debug(s"Using tmp directory $tmpDir")
      ZipUtil.unpack(new ByteArrayInputStream(data.toArray), tmpDir.toFile)
      val newExcels = managed(Files.walk(tmpDir)).acquireAndGet { stream =>
        stream.iterator().asScala.toList
          .filter(f => FilenameUtils.isExtension(f.toString, "xlsx"))
          .filterNot(f => FilenameUtils.getName(f.toString).startsWith("~$"))
          .map { path =>
            context.log.debug(s"Installing excel ${FilenameUtils.getBaseName(path.toString)}")
            excelFactory.getExcel(path, FilenameUtils.getBaseName(path.getFileName.toString))
          }
      }
      currentDirectory.foreach { c => FileUtils.deleteDirectory(c.toFile) }
      currentDirectory = Some(tmpDir)
      excels = newExcels
      this
    }
    case ExcelManager.ReadExcels(replyTo) => {
      replyTo ! ExcelManager.RespondExcels(excels)
      this
    }
  }
}
