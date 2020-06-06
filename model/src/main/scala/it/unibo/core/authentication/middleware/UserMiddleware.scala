package it.unibo.core.authentication.middleware

import io.vertx.core.Handler
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.microservice.vertx._

object UserMiddleware {
  val AUTHORIZATION_HEADER = "Authorization"
  val JWT_TOKEN = "jwt_token"
  def apply() = new UserMiddleware()
  private val bearer = "Bearer"

  def extractToken(authorizationHeader: String): Option[String] = {
    Some(authorizationHeader)
      .filter(_.contains(bearer))
      .map(_.split(" "))
      .flatMap(sequence => sequence.find(_ != bearer))
  }

  def asToken(token : String) : String =  s"$bearer $token"
}

class UserMiddleware private() extends Handler[RoutingContext] {
  import UserMiddleware._
  override def handle(context: RoutingContext): Unit = {
    val pending = context.request().headers().get(AUTHORIZATION_HEADER)
        .flatMap(extractToken)
        .map(token => TokenIdentifier(token))
    pending match  {
      case Some(jwt) => context.put(JWT_TOKEN, jwt); context.next()
      case _ => context.response().setBadRequest()
    }
  }
}
