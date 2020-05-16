package it.unibo.service.authentication

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.ext.auth.jwt.JWTAuth
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.microservice.vertx.BaseVerticle
import it.unibo.core.microservice.vertx._

class AuthenticationVerticle(port : Int = 8081,
                             host : String = "0.0.0.0") extends BaseVerticle(port, host) {

  override def createRouter(): Router = {
    val router = Router.router(vertx)

    router.get("/verify")
        .handler(handleVerifyToken)

    router
  }

  private def handleVerifyToken(context: RoutingContext): Unit = {
    val token = context.queryParam("token").toString()
    currentActiveTokens.find(_.identifier == token) match {
      case Some(value) => context.response().setOk(systemUserToJson(value))
      case None => context.response().setNotAuthorized()
    }
  }

  private def systemUserToJson(user: SystemUser): JsonObject =
    new JsonObject().put("identifier", user.identifier).put("role", user.role)

  private val currentActiveTokens = Seq(
    SystemUser("50" -> "citizen"),
    SystemUser("47" -> "stakeholder"),
    SystemUser("46" -> "doctor")
  )
}
