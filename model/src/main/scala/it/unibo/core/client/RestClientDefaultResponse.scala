package it.unibo.core.client

import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse}
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.utils.{HttpCode, ServiceResponseMapping}

import scala.concurrent.{ExecutionContext, Future}

trait RestClientDefaultResponse {
  self: RestApiClient =>

  def defaultFailHandler[T] : PartialFunction[(HttpCode.Error, String), Fail[_]] = ServiceResponseMapping.httpFailToServiceResponse

  def parseServiceResponseWhenComplete[T, U](future: Future[HttpResponse[T]])(responseMap: PartialFunction[(HttpCode.Success, String), U])(implicit context: ExecutionContext): Future[ServiceResponse[U]] = {
    future.map(response => parseServiceResponse(response, responseMap))
  }

  def parseServiceResponse[T, U](response: HttpResponse[T], responseMap: PartialFunction[(HttpCode.Success, String), U]): ServiceResponse[U] = {
    val httpCode = HttpCode(response.statusCode())
    (httpCode, response.bodyAsString().getOrElse("")) match {
      case (code: HttpCode.Success, content) => Response(responseMap(code, content))
      case (code: HttpCode.Error, content) => defaultFailHandler((code, content))
    }
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(header: (String, String)): HttpRequest[T] = request.putHeader(header._1, header._2)
  }
}
