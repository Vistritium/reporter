package reporter.excel

import java.nio.file.Path

trait ExcelFactory {

  def getExcel(path: Path, name: String): Excel

}
