package reporter.web

import java.lang.reflect.Modifier

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.google.inject.{Inject, Injector, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.reflections.Reflections
import org.reflections.scanners.{SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder}
import reporter.AppSupervisor

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

@Singleton
class WebServer @Inject()(
  implicit val executionContext: ExecutionContext,
  val actorSystem: ActorSystem[AppSupervisor.Command],
  config: Config,
  injector: Injector,
) extends LazyLogging {

  private implicit val classic = actorSystem.toClassic
  private implicit val materialize = ActorMaterializer()

  private val controllers: List[Controller] = {
    val reflections = new Reflections(new ConfigurationBuilder()
      .setUrls(ClasspathHelper.forPackage(getClass.getPackage.getName))
      .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner))
    val set = reflections.getTypesAnnotatedWith(classOf[DiscoverableController]).asScala
      .filterNot(c => Modifier.isAbstract(c.getModifiers))
    logger.info(s"Found following controllers: ${set.mkString("\n", "\n", "")}")
    set.map(c => injector.getInstance(c).asInstanceOf[Controller]).toList
  }

  require(controllers.nonEmpty)
  private val route = controllers.map(_.route).reduce(_ ~ _)

  private val bindingFuture: Unit = {
    val port: Int = config.getInt("web.port")
    Http().bindAndHandle(Route.handlerFlow( route), "0.0.0.0", port)
    logger.info(s"Server started on port $port")
  }
}