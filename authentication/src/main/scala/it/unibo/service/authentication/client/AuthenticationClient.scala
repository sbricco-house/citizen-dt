package it.unibo.service.authentication.client

import java.net.URI

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.client.{RestApiClient, _}
import it.unibo.core.microservice.FutureService
import it.unibo.core.microservice.vertx._
import it.unibo.core.utils.HttpCode
import it.unibo.service.authentication.client.AuthenticationClient._
import it.unibo.service.authentication.model.Resources.AuthenticationInfo
import it.unibo.service.authentication.{AuthenticationService, Token, TokenIdentifier}

object AuthenticationClient {
  val LOGIN = s"/login"
  val VERIFY = s"/verify?token=%s"
  val LOGOUT = s"/logout"
  val REFRESH = s"/refreshToken"

  /**
   * Create an AuthenticationService proxy client using the same interface of the service.
   * @param serviceUri Uri where the real Authentication Service is located
   * @return AuthenticationService instance
   */
  def apply(serviceUri: URI): AuthenticationService = new AuthenticationClient(serviceUri)

  /**
   * Create an AuthenticationService proxy client using the same interface of the service.
   * @param host The http host where the real Authentication Service is located
   * @param port The http port where the real Authentication Service is located
   * @return AuthenticationService instance
   */
  def apply(host: String, port: Int = 8080): AuthenticationService = new AuthenticationClient(URI.create(s"http://$host:$port"))
}

/**
 * Authentication client implementation based on vertx webclient. It follow the [[AuthenticationService]] contract.
 * @param serviceUri  Uri where the real Authentication Service is located.
 */
private class AuthenticationClient(serviceUri: URI) extends AuthenticationService with RestApiClient with RestClientServiceResponse {

  private val vertx = Vertx.vertx()
  private val clientOptions =  WebClientOptions()
    .setFollowRedirects(true)
    .setDefaultPort(serviceUri.getPort)

  override val webClient: WebClient = WebClient.create(vertx, clientOptions)

  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def login(email: String, password: String): FutureService[AuthenticationInfo] = {
    val request = s"${serviceUri.toString}$LOGIN"
    val requestBody = Json.emptyObj().put("email", email).put("password", password)
    parseServiceResponseWhenComplete(webClient.post(request).sendJsonObjectFuture(requestBody)) {
      case (HttpCode.Created, authenticationInfo) => parseAuthenticationInfo(new JsonObject(authenticationInfo))
    }.toFutureService
  }

  override def verifyToken(identifier: TokenIdentifier): FutureService[SystemUser] = {
    val request = s"${serviceUri.toString}$VERIFY".format(identifier.token)
    parseServiceResponseWhenComplete(webClient.get(request).sendFuture()) {
      case (HttpCode.Ok, body) => parseUser(Json.fromObjectString(body))
    }.toFutureService
  }

  override def refresh(identifier: TokenIdentifier): FutureService[Token] = {
    val request = s"${serviceUri.toString}$REFRESH"
    parseServiceResponseWhenComplete(webClient.post(request).putHeader(getAuthorizationHeader(identifier)).sendFuture()) {
      case (HttpCode.Created, newToken) => parseToken(new JsonObject(newToken))
    }.toFutureService
  }

  override def logout(identifier: TokenIdentifier): FutureService[Boolean] = {
    val request = s"${serviceUri.toString}$LOGOUT"
    parseServiceResponseWhenComplete(webClient.get(request).putHeader(getAuthorizationHeader(identifier)).sendFuture()) {
      case (HttpCode.NoContent, "") => true
    }.toFutureService
  }

  private def getAuthorizationHeader(token: TokenIdentifier): (String, String) = "Authorization" -> s"Bearer ${token.token}"

  protected def parseAuthenticationInfo(jsonObject: JsonObject): AuthenticationInfo = {
    AuthenticationInfo(parseToken(jsonObject), parseUser(jsonObject))
  }

  protected def parseToken(jsonObject: JsonObject): Token = {
    Token(jsonObject.getString("token"), jsonObject.getInteger("expirationInMinute"))
  }

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