package it.unibo.service.authentication

import java.security.MessageDigest

import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import java.math.BigInteger

import it.unibo.core.protocol.ServiceError.{BadParameter, Unauthorized}

import scala.collection.mutable

trait TokenIdentifier {
  def token: String
}
case class JWToken(val token: String) extends TokenIdentifier

class AuthenticationServiceBackend(provider: JWTAuth,
                                   userStorage: Storage[SystemUser, String]) extends AuthenticationService {

  private implicit val context: ExecutionContext = ExecutionContext.global
  private val hash = MessageDigest.getInstance("SHA-256")
  private val blackListToken: mutable.LinkedHashSet[TokenIdentifier] = mutable.LinkedHashSet()

  override def login(email: String, password: String): FutureService[TokenIdentifier] = {
    val response = loginUser(email, hashPassword(password))
      .map(user => provider.generateToken(userToClaims(user)))
      .map(token => JWToken(token))
    FutureService(response)
  }

  override def getAuthenticatedUser(identifier: TokenIdentifier): FutureService[SystemUser] = identifier match {
    case JWToken(token) if !blackListToken.contains(identifier) =>
      provider.authenticateFuture(new JsonObject().put("jwt", token))
        .map(user => claimsToUser(user.principal()))
        .map(Response.apply)
        .toFutureService
    case JWToken(_) => FutureService.fail(Unauthorized(s"Invalid or expired token"))
    case _ => FutureService.fail(BadParameter(s"Identifier not supported"))
  }

  override def logout(identifier: TokenIdentifier): FutureService[Boolean] = {
    getAuthenticatedUser(identifier)
      .flatMap(_ => FutureService(insertBlackList(identifier)))
  }

  // TODO: implement refresh token
  override def refresh(authenticated: SystemUser): FutureService[SystemUser] = ???

  private def claimsToUser(user: JsonObject): SystemUser = {
    SystemUser(
      user.getString("email"),
      user.getString("username"),
      "", // no password,
      user.getString("identifier"),
      user.getString("role")
    )
  }

  private def userToClaims(user: SystemUser): JsonObject = {
    new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
  }

  private def hashPassword(password: String): String = {
    val digest = hash.digest(password.getBytes("UTF-8"))
    String.format("%064x", new BigInteger(1, digest))
  }

  private def loginUser(email: String, digest: String): ServiceResponse[SystemUser] = {
    userStorage.find(u => u.email == email && u.password == digest) match {
      case Success(Some(user)) => Response(user)
      case Success(None) => Fail(Unauthorized(s"Invalid email or password"))
      case Failure(exception) => Fail(exception)
    }
  }

  private def insertBlackList(token: TokenIdentifier): ServiceResponse[Boolean] = {
    blackListToken.add(token)
    Response(true)
  }
}
