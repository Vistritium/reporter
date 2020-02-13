package reporter

import com.google.inject.Guice
import reporter.configuration.MainModule
import reporter.web.WebServer
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val injector = Guice.createInjector(new MainModule(config))
    injector.getInstance(classOf[Starter]).start()
  //  injector.getInstance(classOf[WebServer])
  }

}
