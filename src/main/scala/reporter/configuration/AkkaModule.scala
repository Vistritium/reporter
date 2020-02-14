package reporter.configuration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.http.scaladsl.{Http, HttpExt}
import com.google.inject.{Injector, Provides, Singleton}
import net.codingwell.scalaguice.ScalaModule
import reporter.AppSupervisor
import akka.actor.typed.scaladsl.adapter._
import akka.util.Timeout
import reporter.excel.ExcelManager

import scala.concurrent.Await
import scala.concurrent.duration._

@Configuration
class AkkaModule extends ScalaModule {

  implicit val timeout: Timeout = Timeout(3.seconds)

  @Provides
  @Singleton
  def system(injector: Injector): ActorSystem[AppSupervisor.Command] = ActorSystem({
    Behaviors.setup(ctx => new AppSupervisor(ctx, injector))
  }, "system")

  @Provides
  @Singleton
  def httpClient(actorSystem: ActorSystem[AppSupervisor.Command]): HttpExt = Http()(actorSystem.toClassic)
  import akka.actor.typed.scaladsl.AskPattern._
/*
  def excelManager(
    implicit system: ActorSystem[AppSupervisor.Command],
  ): ActorRef[ExcelManager.Command] = {

    Await.result(system.ask(AppSupervisor.GetExcelManager), 3.seconds)
  }
*/


}
