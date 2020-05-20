package it.unibo.core.microservice.vertx

import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.utils.HttpCode

import scala.concurrent.ExecutionContext

trait RestDefaultResponse {
  self: RestApi =>

  def errorMapping[T] : PartialFunction[Fail[T], (HttpCode.Error, String)]

  implicit class RichRoutingContext(context: RoutingContext) {

    def sendServiceResponse[A](serviceResponse: ServiceResponse[A])(mapResponse: Response[A] => (HttpCode.Success, String)): Unit = serviceResponse match {
      case response @ Response(_) => context.response().setResponse(mapResponse(response))
      case error @ Fail(_) => context.response().setResponse(errorMapping(error))
    }

    def sendServiceResponseFromFuture[A](future: FutureService[A])(mapResponse: Response[A] => (HttpCode.Success, String))(implicit executor: ExecutionContext): Unit = {
      future.whenComplete(response => sendServiceResponse(response)(mapResponse))
    }
  }
}
