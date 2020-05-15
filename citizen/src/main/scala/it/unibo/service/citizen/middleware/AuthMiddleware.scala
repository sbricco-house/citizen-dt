package it.unibo.service.citizen.middleware

import io.vertx.core.Handler
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.microservice.vertx._
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.AuthService

import scala.util.Success

object AuthMiddleware {
  private val AUTHORIZATION_HEADER = "Authorization"
  val AUTHENTICATED_USER = "authenticated_user"
  def apply(authService: AuthService) = new AuthMiddleware(authService)
}

class AuthMiddleware private(private val auth: AuthService) extends Handler[RoutingContext] {
  override def handle(context: RoutingContext): Unit = {
    import AuthMiddleware._
    implicit val executionContext = VertxExecutionContext(context.vertx().getOrCreateContext())

    val pending = context.request().headers().get(AUTHORIZATION_HEADER).map(auth.getAuthenticatedUser)
        .getOrElse(FutureService.fail(Unauthorized("Invalid Authorization Header")))

    pending.whenComplete {
      case Response(user) => context.put(AUTHENTICATED_USER, user); context.next()
      case Fail(Unauthorized(m)) => context.response().setNotAuthorized(m)
      case _ => context.response().setInternalError()
    }
  }
}
