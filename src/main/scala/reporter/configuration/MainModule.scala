package reporter.configuration

import akka.actor.typed.ActorSystem
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import net.codingwell.scalaguice.ScalaModule
import org.reflections.Reflections
import reporter.AppSupervisor

import scala.concurrent.ExecutionContext

class MainModule(config: Config) extends ScalaModule with LazyLogging {

  override def configure(): Unit =
    new Reflections(getClass.getPackage.getName)
      .getTypesAnnotatedWith(classOf[Configuration])
      .forEach { c =>
        logger.debug(s"Installing $c")
        install(c.newInstance().asInstanceOf[AbstractModule])
      }

  @Provides
  @Singleton
  def provideConfig(): Config = config

  @Provides
  @Singleton
  def mapper(): ScalaObjectMapper = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.enable(SerializationFeature.INDENT_OUTPUT)
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    mapper
  }

  @Provides
  @Singleton
  def objectMapper(scalaObjectMapper: ScalaObjectMapper): ObjectMapper = {
    scalaObjectMapper.asInstanceOf[ObjectMapper]
  }

  @Provides
  @Singleton
  def executionContext(actorSystem: ActorSystem[AppSupervisor.Command]): ExecutionContext = actorSystem.executionContext



}
