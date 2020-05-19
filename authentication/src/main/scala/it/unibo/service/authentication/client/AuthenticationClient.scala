package it.unibo.service.authentication.client

import java.net.URI

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.client.{RestApiClient, _}
import it.unibo.core.microservice.vertx._
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.service.authentication.{AuthenticationService, JWToken, TokenIdentifier}
import AuthenticationClient._

object AuthenticationClient {
  val LOGIN = s"/login"
  val VERIFY = s"/verify?token=%s"
  val LOGOUT = s"/logout"
  val REFRESH = s"/refreshToken"
}

class AuthenticationClient(serviceUri: URI) extends AuthenticationService with RestApiClient {

  private val clientOptions =  WebClientOptions()
    .setFollowRedirects(true)
    .setDefaultPort(serviceUri.getPort)

  private val vertx = Vertx.vertx()
  private val client: WebClient = WebClient.create(vertx, clientOptions)
  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def login(email: String, password: String): FutureService[TokenIdentifier] = {
    val request = s"${serviceUri.toString}$LOGIN"
    val requestBody = Json.emptyObj().put("email", email).put("password", password)
    client.post(request).sendJsonObjectFuture(requestBody)
      .map(response => response.mapToServiceResponse {
        case (201, token) => Response(JWToken(token))
      })
      .toFutureService
  }

  override def getAuthenticatedUser(identifier: TokenIdentifier): FutureService[SystemUser] = {
    val request = s"${serviceUri.toString}$VERIFY".format(identifier.token)
    client.get(request).sendFuture()
      .map(response => response.mapToServiceResponse {
        case (200, body) => Response(parseUser(Json.fromObjectString(body)))
      }).toFutureService
  }

  override def refresh(identifier: TokenIdentifier): FutureService[TokenIdentifier] = {
    val request = s"${serviceUri.toString}$REFRESH"
    client.post(request).putHeader(getAuthorizationHeader(identifier)).sendFuture()
      .map(response => response.mapToServiceResponse {
        case (201, newToken) => Response(JWToken(newToken))
      }).toFutureService
  }

  override def logout(identifier: TokenIdentifier): FutureService[Boolean] = {
    val request = s"${serviceUri.toString}$LOGOUT"
    client.get(request).putHeader(getAuthorizationHeader(identifier)).sendFuture()
      .map(response => response.mapToServiceResponse {
        case (204, "") => Response(true)
      }).toFutureService
  }

  private def getAuthorizationHeader(token: TokenIdentifier): (String, String) = "Authorization" -> s"Bearer ${token.token}"

  protected def parseUser(jsonObject: JsonObject): SystemUser = {
    SystemUser(
      jsonObject.getAsString("email").getOrElse(""),
      jsonObject.getAsString("username").getOrElse(""),
      jsonObject.getAsString("password").getOrElse(""),
      jsonObject.getAsString("identifier").getOrElse(""),
      jsonObject.getAsString("role").getOrElse("")
    )
  }

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
}