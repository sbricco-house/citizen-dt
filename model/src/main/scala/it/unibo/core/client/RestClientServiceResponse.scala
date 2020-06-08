package it.unibo.core.client

import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse}
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.utils.{HttpCode, ServiceResponseMapping}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Decoration of [[RestApiClient]] that add some functionality
 * to handle in a simple manner the HttpCode as ServiceResponse.
 * @see [[HttpCode]] [[ServiceResponse]] [[ServiceResponseMapping]]
 */
trait RestClientServiceResponse {
  self: RestApiClient =>

  /**
   * The default error handler for map an Http Error to ServiceResponse Fail.
   * @return The default mapping function. The default one is [[ServiceResponseMapping.httpFailToServiceResponse]]
   */
  def defaultFailHandler: PartialFunction[(HttpCode.Error, String), Fail[_]] = ServiceResponseMapping.httpFailToServiceResponse

  /**
   * Map a Future that contains an HttpResponse[T] to a Future that contains a ServiceResponse[U]
   * in the same manner of [[parseServiceResponse()]]
   * @param future Future to be mapped.
   * @param responseMap A mapping function for handle the success case ([[HttpCode.Success]]).
   * @param context Implicit execution context where execute the map of the Future.
   * @tparam T Type parameter of HttpResponse
   * @tparam U Type parameter of ServiceResponse
   * @return
   */
  def parseServiceResponseWhenComplete[T, U](future: Future[HttpResponse[T]])(responseMap: PartialFunction[(HttpCode.Success, String), U])(implicit context: ExecutionContext): Future[ServiceResponse[U]] = {
    future.map(response => parseServiceResponse(response, responseMap))
  }

  /**
   * Parse the HttpResponse[T] to a ServiceResponse[U] taking a function for map the success case
   * and using the [[defaultFailHandler]] as handler for the error code.
   * @param response The vertx HttpResponse to be mapped
   * @param responseMap A mapping function for handle the success case ([[HttpCode.Success]]).
   * @tparam T Type parameter of HttpResponse
   * @tparam U Type parameter of ServiceResponse
   * @return A ServiceResponse[U] right mapped.
   */
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
