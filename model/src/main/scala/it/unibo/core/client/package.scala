package it.unibo.core

import io.vertx.core.buffer.Buffer
import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse}
import it.unibo.core.microservice.ServiceResponse
import it.unibo.core.protocol.ServiceResponseMapping

package object client {
  implicit class RichHttpClientResponse(response: HttpResponse[Buffer]) {
    def mapToServiceResponse[T](f: PartialFunction[(Int, String), ServiceResponse[T]]): ServiceResponse[T] = {
      f.orElse(ServiceResponseMapping.httpToServiceResponse).apply((response.statusCode(), response.bodyAsString().getOrElse("")))
    }
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(header: (String, String)): HttpRequest[T] = request.putHeader(header._1, header._2)
  }
}
