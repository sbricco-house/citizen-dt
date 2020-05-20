package it.unibo.core.microservice.vertx

import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.utils.{HttpCode, ServiceResponseMapping}

import scala.concurrent.ExecutionContext

trait RestServiceResponse {
  self: RestApi =>

  def defaultFailHandler[T] : PartialFunction[Fail[T], (HttpCode.Error, String)] = ServiceResponseMapping.serviceResponseFailToHttp

  def sendServiceResponseWhenComplete[A](context: RoutingContext, future: FutureService[A])(mapResponse: Response[A] => (HttpCode.Success, String))(implicit executor: ExecutionContext): Unit = {
    future.whenComplete(response => sendServiceResponse(context, response)(mapResponse))
  }

  def sendServiceResponse[A](context: RoutingContext, serviceResponse: ServiceResponse[A])(mapResponse: Response[A] => (HttpCode.Success, String)): Unit = {
    serviceResponse match {
      case response @ Response(_) => context.response().setResponse(mapResponse(response))
      case error @ Fail(_) => context.response().setResponse(defaultFailHandler(error))
    }
  }
}
