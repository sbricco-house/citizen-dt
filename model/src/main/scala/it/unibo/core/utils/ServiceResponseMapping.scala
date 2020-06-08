package it.unibo.core.utils

import it.unibo.core.microservice.Fail
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthenticated, Unauthorized}

/**
 * Default mapping from HTTP error code to ServiceResponse Fail(_).
 * It allow to achieve a simple error handling, creating a mapping between a low level Http Response
 * to an high representation level ServiceResponse
 * @see [[HttpCode]] [[it.unibo.core.microservice.ServiceResponse]]
 */
object ServiceResponseMapping {

  /**
   * Mapping between Http Error and Service Response.
   * The inverse function is [[serviceResponseFailToHttp]]
   *
   * @return A partial function that receive as input an HttpCode.Error with a message
   *         and produce a Fail(message) Service Response
   * @note It's partial function because the success case need to be
   *       handled by the user for other purpose (like parsing).
   */
  def httpFailToServiceResponse : PartialFunction[(HttpCode.Error, String), Fail[_]] = {
    case (HttpCode.BadRequest, body) => Fail(MissingParameter(body))
    case (HttpCode.Unauthorized, body) => Fail(Unauthenticated(body))
    case (HttpCode.Forbidden, body) => Fail(Unauthorized(body))
    case (HttpCode.NotFound, body) => Fail(MissingResource(body))
    case (_, body) => Fail(body)
  }

  /**
   * Mapping between Service Response and Http Error.
   * The inverse function is [[httpFailToServiceResponse]]
   *
   * @tparam T Type of Fail[T] content
   * @return A partial function that receive as input Fail(e)
   *         and produce a pair of HttpCode.Error and message of error "e"
   * @note It's partial function because the success case need to be
   *       handled by the user for other purpose (like parsing).
   */
  def serviceResponseFailToHttp[T] : PartialFunction[Fail[T], (HttpCode.Error, String)] = {
    case Fail(MissingParameter(m)) => (HttpCode.BadRequest, m)
    case Fail(Unauthenticated(m)) => (HttpCode.Unauthorized, m)
    case Fail(Unauthorized(m)) => (HttpCode.Forbidden, m)
    case Fail(MissingResource(m)) => (HttpCode.NotFound, m)
    case Fail(e) => (HttpCode.InternalError, e.toString)
  }

}
