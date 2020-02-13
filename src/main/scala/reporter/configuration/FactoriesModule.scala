package reporter.configuration

import com.google.inject.assistedinject.FactoryModuleBuilder
import net.codingwell.scalaguice.ScalaModule
import reporter.datasource.{SlickDataSource, SlickDataSourceFactory}
import reporter.excel.ExcelFactory

@Configuration
class FactoriesModule extends ScalaModule {
  override def configure(): Unit = {

    install(new FactoryModuleBuilder().build(classOf[SlickDataSourceFactory]))
    install(new FactoryModuleBuilder().build(classOf[ExcelFactory]))

  }
}
