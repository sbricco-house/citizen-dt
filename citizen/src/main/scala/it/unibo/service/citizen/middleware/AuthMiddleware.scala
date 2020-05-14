package it.unibo.service.citizen.middleware

import io.vertx.core.Handler
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.microservice.vertx._
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

    val pendingResponse = context.request().headers().get(AUTHORIZATION_HEADER).map(auth.getAuthenticatedUser)
    pendingResponse match  {
      case Some(response) => response.onComplete {
        case Success(user) => context.put(AUTHENTICATED_USER, user); context.next()
        case _ => context.response().setNotAuthorized("Invalid Authorization Token")
      }
      case _ => context.response().setNotAuthorized("Invalid Authorization Header")
    }
  }
}
