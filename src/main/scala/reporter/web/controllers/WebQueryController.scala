package reporter.web.controllers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import reporter.QueryHelper
import reporter.datasource.SlickDataSourceFactory
import reporter.web.{Controller, DiscoverableController}

import scala.collection.JavaConverters._

case class WebQuery(
  name: String,
  database: String,
  query: String,
)

@Singleton
@DiscoverableController
class WebQueryController @Inject()(
  config: Config,
  slickDataSourceFactory: SlickDataSourceFactory,
  querySource: QueryHelper
) extends Controller with LazyLogging {

  import net.ceedubs.ficus.Ficus._
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._

/*
  val webQueries = config.getConfigList("processes")
    .asScala.filter(_.getString("type") == "WEB_QUERY")
    .map(_.as[WebQuery])
    .map { webQuery =>
      get & path(webQuery.name) {
        val query = querySource.get(webQuery.query, Map.empty)
        val source = slickDataSourceFactory.get(webQuery.database, query)


          ???
      }
    }
*/

  override def route: Route = ???


}
