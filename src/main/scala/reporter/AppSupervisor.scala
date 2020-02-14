package reporter

import akka.actor.Props
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal, SpawnProtocol}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.google.inject.Injector
import reporter.excel.{ExcelFactory, ExcelManager}


object AppSupervisor {

  trait Command

  final case class GetExcelManager(replyTo: ActorRef[ActorRef[ExcelManager.Command]]) extends Command

}


class AppSupervisor(
  context: ActorContext[AppSupervisor.Command],
  injector: Injector
) extends AbstractBehavior[AppSupervisor.Command](context) {
  context.log.info("Typed supervisor started")

  val excelManager: ActorRef[ExcelManager.Command] = context.spawn(Behaviors.setup(ctx => new ExcelManager(
    ctx,
    injector.getInstance(classOf[ExcelFactory]))), "excel-manager"
  )

  override def onMessage(msg: AppSupervisor.Command): Behavior[AppSupervisor.Command] = msg match {
    case AppSupervisor.GetExcelManager(replyTo) => {
      replyTo.tell(excelManager)
      this
    }
    case _ => this
  }

  override def onSignal: PartialFunction[Signal, Behavior[AppSupervisor.Command]] = {
    case PostStop =>
      context.log.info("Typed supervisor stopped")
      this
  }
}