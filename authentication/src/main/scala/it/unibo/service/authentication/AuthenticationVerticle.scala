package it.unibo.service.authentication

import io.vertx.scala.ext.auth.jwt.JWTAuth
import io.vertx.scala.ext.web.Router
import it.unibo.core.microservice.vertx.BaseVerticle

class AuthenticationVerticle(port : Int = 8080,
                             host : String = "0.0.0.0") extends BaseVerticle(port, host) {

  override def createRouter(): Router = {
    val router = Router.router(vertx)

    router
  }
}
