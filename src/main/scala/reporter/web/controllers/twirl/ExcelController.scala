package reporter.web.controllers.twirl

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpHeader, HttpResponse, MediaTypes, ResponseEntity}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.MediaType
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.inject.{Inject, Singleton}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import reporter.datasource.SlickDataSourceFactory
import reporter.excel.ExcelManager
import reporter.web.{Controller, DiscoverableController, TwirlController}
import reporter.{AppSupervisor, QueryHelper}

import scala.concurrent.ExecutionContext


@Singleton
@DiscoverableController
class ExcelController @Inject()(
  config: Config,
  slickDataSourceFactory: SlickDataSourceFactory,
  querySource: QueryHelper,
  implicit val system: ActorSystem[AppSupervisor.Command],
  implicit val executionContext: ExecutionContext,
) extends TwirlController with LazyLogging {

  import akka.actor.typed.scaladsl.AskPattern._

  override def route: Route =
    pathSingleSlash {
      onSuccess(for {
        manager <- system.ask(AppSupervisor.GetExcelManager)
        excels <- manager.ask(ExcelManager.ReadExcels)
      } yield excels) { excels =>
        complete(html.index.render(excels.excels.map(_.metadata)))
      }
    } ~ path("upload") {
      complete(html.upload.render(None))
    } ~ pathPrefix("excel") {
      post {
        fileUpload("zippedExcels") { case (_, bytes) =>
          onSuccess(for {
            bytes <- bytes.runReduce(_ ++ _)
            manager <- system.ask(AppSupervisor.GetExcelManager)
          } yield {
            manager ! (ExcelManager.ExcelsZip(bytes))
          }) {
            complete(html.upload.render(Some("Done")))
          }
        }
      } ~ (get | post) {
        path(Segment) { excelName =>
          onSuccess(for {
            manager <- system.ask(AppSupervisor.GetExcelManager)
            excels <- manager.ask(ExcelManager.ReadExcels)
          } yield excels) { excels =>
            excels.excels.find(_.name == excelName) match {
              case None => complete(s"Excel ${excelName} does not exist")
              case Some(excel) => {
                formFieldMap { formParams =>
                  val requiredParams = excel.metadata.tables.flatMap(_.query.params).distinct
                  val filledParams = requiredParams
                    .flatMap { param => formParams.get(param).map(value => param -> value) }
                  if (requiredParams.size != filledParams.size) {
                    complete(html.params.render(requiredParams, excelName))
                  } else {
                    val source = excel.generate(filledParams.toMap)
                    complete(HttpResponse(
                      headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$excelName.xlsx"))),
                      entity = HttpEntity.CloseDelimited(MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, source)
                    ))
                  }
                }
              }
            }
          }
        }
      }
    }


}
