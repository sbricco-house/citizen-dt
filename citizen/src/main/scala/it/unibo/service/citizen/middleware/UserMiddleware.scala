package it.unibo.service.citizen.middleware

import io.vertx.core.Handler
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.microservice.vertx._
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.AuthService

import scala.util.Success

object UserMiddleware {
  private val AUTHORIZATION_HEADER = "Authorization"
  val AUTHENTICATED_USER = "authenticated_user"
  def apply() = new UserMiddleware()
}

class UserMiddleware private() extends Handler[RoutingContext] {
  override def handle(context: RoutingContext): Unit = {
    import UserMiddleware._
    implicit val executionContext = VertxExecutionContext(context.vertx().getOrCreateContext())

    val pending = context.request().headers().get(AUTHORIZATION_HEADER)
    pending match  {
      case Some(user) => context.put(AUTHENTICATED_USER, user); context.next()
      case _ => context.response().setBadRequest()
    }
  }
}
