package reporter.excel

import java.nio.file.{Files, Path, StandardCopyOption}

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import resource.managed

@Singleton
class ExcelManager @Inject()(
  excelFactory: ExcelFactory
) extends LazyLogging {

  val excels: List[Excel] = List("budget").map { excelTemplateName =>
    val templatePath = s"excel-templates/$excelTemplateName.xlsx"
    val template: Path = managed(getClass.getClassLoader.getResourceAsStream(templatePath)).acquireAndGet { template =>
      require(template != null, s"resource $templatePath cannot be found")
      val path = Files.createTempFile(s"reporter_tmpl_$excelTemplateName", ".xlsx")
      Files.copy(template, path, StandardCopyOption.REPLACE_EXISTING)
      logger.info(s"Using temporary file $path")
      path
    }
    excelFactory.getExcel(template, excelTemplateName)
  }

}
