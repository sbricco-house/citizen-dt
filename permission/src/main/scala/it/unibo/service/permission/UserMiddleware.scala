package it.unibo.service.permission

import java.util.concurrent.Executors

import io.vertx.core.Handler
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.jwt.JWTAuth
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.authentication.AuthenticationParsers
import it.unibo.core.microservice.vertx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
object UserMiddleware {
  val AUTHORIZATION_HEADER = "Authorization"
  val USER = "user"
  def apply(provider: JWTAuth, vertx: Vertx) = new UserMiddleware(provider, vertx)
}

class UserMiddleware private(provider: JWTAuth, vertx : Vertx) extends Handler[RoutingContext] {
  private implicit val context: ExecutionContext = VertxExecutionContext.apply(vertx.getOrCreateContext())

  override def handle(context: RoutingContext): Unit = {
    val pending = context.request().headers()
      .get(UserMiddleware.AUTHORIZATION_HEADER)
      .map(token => Json.obj("jwt" -> token))
      .map(jwt => provider.authenticateFuture(jwt))

    pending match  {
      case Some(user) =>
        user.transform {
          case Success(user) => AuthenticationParsers.SystemUserParser
            .decode(user.principal())
            .map(Success(_))
            .getOrElse(Failure(new IllegalArgumentException))
          case other => other
        }.onComplete {
          case Success(systemUser) =>
            context.put(UserMiddleware.USER, systemUser);
            context.next()
          case other => context.response().setBadRequest()
        }
      case _ => context.response().setBadRequest()
    }
  }
}
