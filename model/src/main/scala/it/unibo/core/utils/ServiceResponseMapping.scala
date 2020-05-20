package it.unibo.core.utils

import it.unibo.core.microservice.Fail
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthenticated, Unauthorized}

object ServiceResponseMapping {

  def httpFailToServiceResponse : PartialFunction[(HttpCode.Error, String), Fail[_]] = {
    case (HttpCode.BadRequest, body) => Fail(MissingParameter(body))
    case (HttpCode.Unauthorized, body) => Fail(Unauthenticated(body))
    case (HttpCode.Forbidden, body) => Fail(Unauthorized(body))
    case (HttpCode.NotFound, body) => Fail(MissingResource(body))
    case (_, body) => Fail(body)
  }

  def serviceResponseFailToHttp[T] : PartialFunction[Fail[T], (HttpCode.Error, String)] = {
    case Fail(MissingParameter(m)) => (HttpCode.BadRequest, m)
    case Fail(Unauthenticated(m)) => (HttpCode.Unauthorized, m)
    case Fail(Unauthorized(m)) => (HttpCode.Forbidden, m)
    case Fail(MissingResource(m)) => (HttpCode.NotFound, m)
    case Fail(e) => (HttpCode.InternalError, e.toString)
  }

}
