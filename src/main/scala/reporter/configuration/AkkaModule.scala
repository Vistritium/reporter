package reporter.configuration

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import com.google.inject.{Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule
import reporter.AppSupervisor
import akka.actor.typed.scaladsl.adapter._

@Configuration
class AkkaModule extends ScalaModule {

  @Provides
  @Singleton
  def system(): ActorSystem[Nothing] = ActorSystem(AppSupervisor(), "system")

  @Provides
  @Singleton
  def httpClient(actorSystem: ActorSystem[_]): HttpExt = Http()(actorSystem.toClassic)

}
