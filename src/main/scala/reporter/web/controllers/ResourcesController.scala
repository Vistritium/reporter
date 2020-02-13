package reporter.web.controllers

import akka.http.scaladsl.server.Route
import com.google.inject.{Inject, Singleton}
import reporter.web.{Controller, DiscoverableController}
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.CacheDirectives._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration._

@Singleton
@DiscoverableController
class ResourcesController @Inject()(
) extends Controller with LazyLogging {
  override def route: Route =
    respondWithHeader(`Cache-Control`(`public`, `max-age`(1.hour.toSeconds))) {
      pathPrefix("web") {
        getFromResourceDirectory("web", getClass.getClassLoader)
      } ~ get {
        pathPrefix("favicon.ico") {
          getFromResource("web/favicon.ico")
        }
      }
    }
}
