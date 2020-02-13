package reporter.datasource

import com.google.inject.assistedinject.Assisted

trait SlickDataSourceFactory {

  def get(@Assisted("dbConfigPath") dbConfigPath: String, @Assisted("query") query: String): SlickDataSource

}
