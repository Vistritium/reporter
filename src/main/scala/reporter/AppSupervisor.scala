package reporter
import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object AppSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new AppSupervisor(context))
}

class AppSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("Typed supervisor started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("Typed supervisor stopped")
      this
  }
}