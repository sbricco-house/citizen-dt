package it.unibo.service.authentication
import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.data.Storage
import it.unibo.service.authentication.AuthService.MockUser

import scala.concurrent.{ExecutionContext, Future}

class VertxJwtAuthService(val provider: JWTAuth,
                          val storage: Storage[String, SystemUser]) extends AuthService {

  private implicit val executionContext = ExecutionContext.global

  override def getAuthenticatedUser(identifier: String): Future[SystemUser] = {
    val authObject = new JsonObject().put("jwt", identifier)
    provider.authenticateFuture(authObject).map(user => principalToSystemUser(user.principal()))
  }

  private def principalToSystemUser(principal: JsonObject): SystemUser = {
    // TODO: extract info from previously saved user into provider
    MockUser
  }
}
