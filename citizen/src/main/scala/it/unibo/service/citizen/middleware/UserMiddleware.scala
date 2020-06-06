package it.unibo.service.citizen.middleware

import io.vertx.core.Handler
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.microservice.vertx._
import it.unibo.service.citizen.middleware.UserMiddleware._

object UserMiddleware {
  val AUTHORIZATION_HEADER = "Authorization"
  val JWT_TOKEN = "jwt_token"
  def apply() = new UserMiddleware()
}

class UserMiddleware private() extends Handler[RoutingContext] {
  override def handle(context: RoutingContext): Unit = {
    val pending = context.request().headers().get(AUTHORIZATION_HEADER)
        .flatMap(extractToken)
        .map(token => TokenIdentifier(token))
    pending match  {
      case Some(jwt) => context.put(JWT_TOKEN, jwt); context.next()
      case _ => context.response().setBadRequest()
    }
  }

  private def extractToken(authorizationHeader: String): Option[String] = {
    val bearer = "Bearer"
    authorizationHeader.split(bearer)
      .map(_.trim)
      .filter(_.nonEmpty)
      .find(_ != bearer)
  }
}
