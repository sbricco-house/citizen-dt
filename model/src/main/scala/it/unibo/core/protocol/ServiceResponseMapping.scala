package it.unibo.core.protocol

import it.unibo.core.microservice.{Fail, Response, ServiceResponse}
import it.unibo.core.protocol.ServiceError.{MissingResource, Unauthenticated, Unauthorized}

object ServiceResponseMapping {

  def httpToServiceResponse[T] : PartialFunction[(Int, String), ServiceResponse[T]] = {
    case (204, _) => Response("".asInstanceOf[T])
    // TODO: case (400, body) => MissingParameter
    case (401, body) => Fail(Unauthenticated(body))
    case (403, body) => Fail(Unauthorized(body))
    case (404, body) => Fail(MissingResource(body))
    case (500, body) => Fail(body)
  }

  def serviceResponseToHttp[T] : PartialFunction[ServiceResponse[T], (Int, String)] = {
    case Fail(Unauthenticated(m)) => (401, m)
    case Fail(Unauthorized(m)) => (403, m)
    case Fail(MissingResource(m)) => (404, m)
    case Fail(e) => (500, e.toString)
  }

}
