package it.unibo.core

import io.vertx.core.buffer.Buffer
import io.vertx.scala.ext.web.client.HttpResponse
import it.unibo.core.microservice.ServiceResponse
import it.unibo.core.protocol.ServiceResponseMapping

package object client {
  implicit class RichHttpClientResponse(response: HttpResponse[Buffer]) {
    def mapToServiceResponse[T](f: PartialFunction[(Int, String), ServiceResponse[T]]): ServiceResponse[T] = {
      f.orElse(ServiceResponseMapping.httpToServiceResponse).apply((response.statusCode(), response.bodyAsString().getOrElse("")))
    }
  }
}
