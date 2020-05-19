package it.unibo.service.authentication

import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.microservice.vertx.{RestApi, _}
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.protocol.ServiceError._
import it.unibo.core.protocol.ServiceResponseMapping

object RestAuthenticationApi {
  val LOGIN_ENDPOINT = "/login"
  val VERIFY_ENDPOINT = "/verify"
  val LOGOUT_ENDPOINT = "/logout"
  val REFRESH_TOKEN = "/refreshToken"
  val AUTHORIZATION_HEADER = "Authorization"
  val TOKEN_QUERY = "token"
}

trait RestAuthenticationApi extends RestApi {
  self : AuthenticationVerticle =>
  import RestAuthenticationApi._

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
      .getOrElse(FutureService.fail(BadParameter(s"Missing or malformed request body")))

    login.whenComplete {
      case Response(JWToken(token)) => context.response().setCreated(token)
      case Fail(Unauthenticated(m)) => context.response().setNotAuthorized(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case Fail(BadParameter(m)) => context.response().setBadRequest(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handleVerifyToken(context: RoutingContext): Unit = {
    context.queryParams.get(TOKEN_QUERY)
      .map(JWToken.apply)  // TODO: validate if is valid jwt token
      .map(authenticationService.getAuthenticatedUser(_))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing token")))
      .whenComplete {
        case Response(content) => context.response().setOk(systemUserToJson(content))
          // todo: add case when token is expired or invalid
        case Fail(MissingParameter(m)) => context.response().setBadRequest(m)
        case Fail(BadParameter(m)) => context.response().setBadRequest(m)
        case Fail(Unauthenticated(m)) => context.response().setNotAuthorized(m)
        case _ => context.response().setInternalError()
    }
  }

  private def handleLogout(context: RoutingContext): Unit = {
    context.request().headers().get(AUTHORIZATION_HEADER)
      .flatMap(auth => extractToken(auth))
      .map(token => authenticationService.logout(JWToken(token)))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing authorization header")))
      .whenComplete {
        case Response(_) => context.response().setNoContent()
        case Fail(MissingParameter(m)) => context.response().setBadRequest(m)
        case Fail(BadParameter(m)) => context.response().setBadRequest(m)
        case Fail(Unauthenticated(m)) => context.response().setNotAuthorized(m)
        case Fail(Unauthorized(m)) => context.response().setForbidden(m)
        case _ => context.response().setInternalError()
      }
  }

  private def handleRefresh(context: RoutingContext): Unit = {
    context.request().headers().get(AUTHORIZATION_HEADER)
      .flatMap(auth => extractToken(auth))
      .map(token => authenticationService.refresh(JWToken(token)))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing authorization header")))
      .whenComplete {
        case Response(JWToken(token)) => context.response().setCreated(token)
        case Fail(MissingParameter(m)) => context.response().setBadRequest(m)
        case Fail(BadParameter(m)) => context.response().setBadRequest(m)
        case Fail(Unauthenticated(m)) => context.response().setNotAuthorized(m)
        case Fail(Unauthorized(m)) => context.response().setForbidden(m)
        case _ => context.response().setInternalError()
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
