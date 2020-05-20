package it.unibo.core.client

import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse}
import it.unibo.core.microservice.{Fail, Response, ServiceResponse}
import it.unibo.core.utils.HttpCode

trait RestClientDefaultResponse {
  self: RestApiClient =>

  def errorMapping[T] : PartialFunction[(HttpCode.Error, String), Fail[_]]

  implicit class RichHttpResponse[+T](response: HttpResponse[T]) {
     def toServiceResponse[U](responseMap: PartialFunction[(HttpCode.Success, String), U]): ServiceResponse[U] = {
        val httpCode = HttpCode(response.statusCode())
        (httpCode, response.bodyAsString().getOrElse("")) match {
          case (code: HttpCode.Success, content) => Response(responseMap(code, content))
          case (code: HttpCode.Error, content) => errorMapping((code, content))
       }
     }
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(header: (String, String)): HttpRequest[T] = request.putHeader(header._1, header._2)
  }
}
