package it.unibo.service.authentication

import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.microservice.vertx.{RestApi, _}
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.utils.ServiceError.MissingParameter
import it.unibo.core.utils.{HttpCode, ServiceResponseMapping}

object RestAuthenticationApi {
  val LOGIN_ENDPOINT = "/login"
  val VERIFY_ENDPOINT = "/verify"
  val LOGOUT_ENDPOINT = "/logout"
  val REFRESH_TOKEN = "/refreshToken"
  val AUTHORIZATION_HEADER = "Authorization"
  val TOKEN_QUERY = "token"
}

trait RestAuthenticationApi extends RestApi with RestDefaultResponse {
  self : AuthenticationVerticle =>
  import RestAuthenticationApi._

  override def errorMapping[T]: PartialFunction[Fail[T], (HttpCode.Error, String)] =  ServiceResponseMapping.serviceResponseFailToHttp

  override def createRouter: Router = {
    val router = Router.router(vertx)

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

    context.sendServiceResponseFromFuture(login) {
      case Response(TokenIdentifier(token)) => (HttpCode.Created, token)
    }
  }

  private def handleVerifyToken(context: RoutingContext): Unit = {
    val verify = context.queryParams.get(TOKEN_QUERY)
      .map(TokenIdentifier.apply)  // TODO: validate if is valid jwt token
      .map(authenticationService.verifyToken)
      .getOrElse(FutureService.fail(MissingParameter(s"Missing token")))

    context.sendServiceResponseFromFuture(verify) {
      case Response(content) => (HttpCode.Ok, userToJson(content).encode())
    }
  }

  private def handleLogout(context: RoutingContext): Unit = {
    val logout = context.request().headers().get(AUTHORIZATION_HEADER)
      .flatMap(auth => extractToken(auth))
      .map(token => authenticationService.logout(TokenIdentifier(token)))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing authorization header")))

    context.sendServiceResponseFromFuture(logout) {
      case Response(_) => (HttpCode.NoContent, "")
    }
  }

  private def handleRefresh(context: RoutingContext): Unit = {
    val refresh = context.request().headers().get(AUTHORIZATION_HEADER)
      .flatMap(auth => extractToken(auth))
      .map(token => authenticationService.refresh(TokenIdentifier(token)))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing authorization header")))

    context.sendServiceResponseFromFuture(refresh) {
      case Response(TokenIdentifier(token)) => (HttpCode.Created, token)
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
