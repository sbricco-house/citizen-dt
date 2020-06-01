package it.unibo.core.microservice.vertx

import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.utils.{HttpCode, ServiceResponseMapping}

import scala.concurrent.ExecutionContext

/**
 * Decoration of [[RestApi]] that add some functionality
 * to marshall in a simple manner a ServiceResponse to HttpCode.
 * @see [[HttpCode]] [[ServiceResponse]] [[ServiceResponseMapping]]
 */
trait RestServiceResponse {
  self: RestApi =>

  /**
   * The default error handler for map a ServiceResponse Fail[T] to Http error Code.
   * @tparam T Type parameter of Fail[T]
   * @return The default mapping function. The default one is [[ServiceResponseMapping.serviceResponseFailToHttp]]
   */
  def defaultFailHandler[T] : PartialFunction[Fail[T], (HttpCode.Error, String)] = ServiceResponseMapping.serviceResponseFailToHttp

  /**
   * Send and http response starting from a FutureService[A].
   * The http response is defined in the same manner of [[sendServiceResponse()]]
   * @param context RoutingContext of Vertx Router
   * @param future The FutureService used as starting point for produce a response
   * @param mapResponse A mapping function for marshall the content of ServiceResponse to a pair of HttpCode and content
   * @param executor Implicit executor where complete the FutureService before send the http response.
   * @tparam A Type parameter of FutureService[A]
   */
  def sendServiceResponseWhenComplete[A](context: RoutingContext, future: FutureService[A])(mapResponse: Response[A] => (HttpCode.Success, String))(implicit executor: ExecutionContext): Unit = {
    future.whenComplete(response => sendServiceResponse(context, response)(mapResponse))
  }

  /**
   * Send and http response starting from a ServiceResponse[A].
   * The response is defined by the provided mapping function for each success case
   * and by [[defaultFailHandler]] as mapping function for each Fail().
   *
   * @param context RoutingContext of Vertx Router
   * @param serviceResponse The ServiceResponse to be mapped and send
   * @param mapResponse A mapping function for marshall the content of ServiceResponse to a pair of HttpCode and content
   * @tparam A Type parameter of ServiceResponse[A].
   */
  def sendServiceResponse[A](context: RoutingContext, serviceResponse: ServiceResponse[A])(mapResponse: Response[A] => (HttpCode.Success, String)): Unit = {
    serviceResponse match {
      case response @ Response(_) => context.response().setResponse(mapResponse(response))
      case error @ Fail(_) => context.response().setResponse(defaultFailHandler(error))
    }
  }
}
