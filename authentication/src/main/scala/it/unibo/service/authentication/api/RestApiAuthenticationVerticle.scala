package it.unibo.service.authentication.api

import io.vertx.core.json.JsonObject
import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.service.authentication.model.Resources.AuthenticationInfo
import it.unibo.service.authentication.{AuthenticationService, Token}

/**
 * Describe the context (in terms of http vertx platform) in which AuthenticationService are involved.
 * @param authenticationService The logic to manage the user's authentication.
 * @param port The port in which the http server starts
 * @param host The host in which the http server starts
 */
class RestApiAuthenticationVerticle(protected val authenticationService: AuthenticationService,
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
}
