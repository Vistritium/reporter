package reporter

import com.google.inject.{Inject, Singleton}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils

case class QueryContainer(
  query: String
)

case class QueryMeta(
  databaseName: String,
  queryTemplate: String,
  params: List[String]
) {

  def query(paramsWithValues: Map[String, String]): String = {
    require(paramsWithValues.keySet == this.params.toSet, s"${this} - incorrect provided params: $paramsWithValues")
    paramsWithValues.foldLeft(queryTemplate) { case (query, (key, value)) => query.replace(s"$${$key}", value) }
  }

}

@Singleton
class QueryHelper @Inject()(
) extends LazyLogging {

  def info(database: String, query: String): QueryMeta = {
    QueryMeta(database, query, List.empty)
  }

}
