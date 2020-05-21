package it.unibo.service.authentication

import io.vertx.core.json.JsonObject
import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.vertx.BaseVerticle
import it.unibo.core.microservice.vertx._

class AuthenticationVerticle(protected val authenticationService: AuthenticationService,
                             port : Int = 8080,
                             host : String = "0.0.0.0") extends BaseVerticle(port, host) {

  case class LoginUser(email: String, password: String)

  protected def parseLoginUser(jsonObject: JsonObject): Option[LoginUser] = {
    val emailOption = jsonObject.getAsString("email")
    val passwordOption = jsonObject.getAsString("password")
    for {
      email <- emailOption
      password <- passwordOption
    } yield LoginUser(email, password)
  }

  protected def userToJson(user: SystemUser): JsonObject = {
    new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
  }
}
