package reporter.web

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

@DiscoverableController
abstract class Controller extends LazyLogging {

  protected implicit val timeout: Timeout = Timeout(1.hour)

  def route: Route

  def handleFutureError[T](dir: Directive1[Try[T]])(next: T => Route): Route = {
    dir {
      case Failure(exception) => {
        logger.debug("futureError", exception)
        complete(HttpResponse(status = StatusCodes.InternalServerError, entity = s"${exception.getMessage}"))
      }
      case Success(value) => next(value)
    }
  }

}
