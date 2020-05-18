package it.unibo.service.authentication

import io.vertx.core.json.JsonObject
import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.vertx.BaseVerticle
import it.unibo.core.microservice.vertx._

class AuthenticationVerticle(protected val authenticationService: AuthenticationService,
                             port : Int = 8080,
                             host : String = "0.0.0.0") extends BaseVerticle(port, host) {

  protected def parseLoginUser(jsonObject: JsonObject): Option[SystemUser] = {
    val emailOption = jsonObject.getAsString("email")
    val username = jsonObject.getAsString("username")
    val passwordOption = jsonObject.getAsString("password")
    val identifierOption = jsonObject.getAsString("identifier")
    val roleOption = jsonObject.getAsString("role")
    for {
      email <- emailOption
      password <- passwordOption
    } yield SystemUser(email, username.getOrElse(""), password, identifierOption.getOrElse(""), roleOption.getOrElse(""))
  }

  protected def systemUserToJson(user: SystemUser): JsonObject = {
    new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
  }

}
