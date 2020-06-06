package it.unibo.service.authentication.api

import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.authentication.AuthenticationParsers.{AuthInfoParser, SystemUserParser, TokenParser}
import it.unibo.core.authentication.Resources.AuthenticationInfo
import it.unibo.core.authentication.{Token, TokenIdentifier}
import it.unibo.core.microservice.vertx.{RestApi, _}
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.utils.HttpCode
import it.unibo.core.utils.ServiceError.MissingParameter

object RestApiAuthentication {
  val LOGIN_ENDPOINT = "/login"
  val VERIFY_ENDPOINT = "/verify"
  val LOGOUT_ENDPOINT = "/logout"
  val REFRESH_TOKEN = "/refreshToken"
  val AUTHORIZATION_HEADER = "Authorization"
  val TOKEN_QUERY = "token"
}

/**
 * Expose the [[it.unibo.service.authentication.AuthenticationService]] through HTTP Rest API.
 * Work as decoration of [[it.unibo.service.authentication.api.RestApiAuthenticationVerticle]].
 */
trait RestApiAuthentication extends RestApi with RestServiceResponse {
  self : RestApiAuthenticationVerticle =>
  import RestApiAuthentication._

  override def createRouter: Router = {
    val router = Router.router(vertx)
    //CorsSupport.enableTo(router)

    router.post(LOGIN_ENDPOINT)
      .handler(BodyHandler.create())
      .handler(handleLogin)

    router.get(VERIFY_ENDPOINT)
        .handler(handleVerifyToken)

    router.get(LOGOUT_ENDPOINT)
        .handler(handleLogout)

    router.post(REFRESH_TOKEN)
        .handler(handleRefresh)

    router
  }

  private def handleLogin(context: RoutingContext): Unit = {
    val login = context.getBodyAsJson()
      .flatMap(self.parseLoginUser)
      .map(user => authenticationService.login(user.email, user.password))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing or malformed request body")))

    sendServiceResponseWhenComplete(context, login) {
      case Response(e: AuthenticationInfo) => (HttpCode.Created, AuthInfoParser.encode(e).encode())
    }
  }

  private def handleVerifyToken(context: RoutingContext): Unit = {
    val verify = context.queryParams.get(TOKEN_QUERY)
      .map(TokenIdentifier.apply)  // TODO: validate if is valid jwt token
      .map(authenticationService.verifyToken)
      .getOrElse(FutureService.fail(MissingParameter(s"Missing token")))

    sendServiceResponseWhenComplete(context, verify) {
      case Response(content) => (HttpCode.Ok, SystemUserParser.encode(content).encode())
    }
  }

  private def handleLogout(context: RoutingContext): Unit = {
    val logout = context.request().headers().get(AUTHORIZATION_HEADER)
      .flatMap(auth => extractToken(auth))
      .map(token => authenticationService.logout(TokenIdentifier(token)))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing authorization header")))

    sendServiceResponseWhenComplete(context, logout) {
      case Response(_) => (HttpCode.NoContent, "")
    }
  }

  private def handleRefresh(context: RoutingContext): Unit = {
    val refresh = context.request().headers().get(AUTHORIZATION_HEADER)
      .flatMap(auth => extractToken(auth))
      .map(token => authenticationService.refresh(TokenIdentifier(token)))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing authorization header")))

    sendServiceResponseWhenComplete(context, refresh) {
      case Response(t : Token) => (HttpCode.Created, TokenParser.encode(t).encode())
    }
  }

  private def extractToken(authorizationHeader: String): Option[String] = {
    val bearer = "Bearer"
    authorizationHeader.split(bearer)
      .map(_.trim)
      .filter(_.nonEmpty)
      .find(_ != bearer)
  }

}
