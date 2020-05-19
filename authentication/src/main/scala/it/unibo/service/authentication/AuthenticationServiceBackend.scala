package it.unibo.service.authentication

import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.Executors

import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.protocol.ServiceError.{BadParameter, Unauthenticated, Unauthorized}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait TokenIdentifier {
  def token: String
}
case class JWToken(val token: String) extends TokenIdentifier

class AuthenticationServiceBackend(provider: JWTAuth,
                                   userStorage: Storage[SystemUser, String]) extends AuthenticationService {

  private implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  private val hash = MessageDigest.getInstance("SHA-256")
  private val blackListToken: mutable.LinkedHashSet[TokenIdentifier] = mutable.LinkedHashSet()
  private val jwtOptions = JWTOptions()
    .setExpiresInMinutes(30)

  override def login(email: String, password: String): FutureService[TokenIdentifier] = {
    FutureService(loginUser(email, hashPassword(password)))
      .map(user => generateToken(user))
  }

  override def getAuthenticatedUser(identifier: TokenIdentifier): FutureService[SystemUser] = identifier match {
      case JWToken(token) if !blackListToken.contains(identifier) =>
        provider.authenticateFuture(new JsonObject().put("jwt", token))
          .map(user => claimsToUser(user.principal()))
          .map(Response.apply)
          .toFutureService
      case JWToken(_) => FutureService.fail(Unauthenticated(s"Invalid or expired token"))
      case _ => FutureService.fail(BadParameter(s"Identifier not supported"))
  }

  override def logout(identifier: TokenIdentifier): FutureService[Boolean] = {
    getAuthenticatedUser(identifier)
      .flatMap(_ => FutureService(insertBlackList(identifier)))
  }

  override def refresh(identifier: TokenIdentifier): FutureService[TokenIdentifier] = {
    // authenticate the user, add current token to blacklist, regenerate the token
    getAuthenticatedUser(identifier)
      .map(user => {
        insertBlackList(identifier)
        generateToken(user)
      })
  }

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

  private def generateToken(user: SystemUser): TokenIdentifier = {
    // introduce fixed delay for prevent creation of same jwt. By definition JWT time is expressed in seconds
    // generating a token for the same user at high rate < 1s could generate same token. In real scenario
    // this not happens, but is better to prevent this
    Thread.sleep(1000)
    val token = provider.generateToken(userToClaims(user), jwtOptions)
    JWToken(token)
  }

  private def hashPassword(password: String): String = {
    val digest = hash.digest(password.getBytes("UTF-8"))
    String.format("%064x", new BigInteger(1, digest))
  }

  private def loginUser(email: String, digest: String): ServiceResponse[SystemUser] = {
    userStorage.find(u => u.email == email && u.password == digest) match {
      case Success(Some(user)) => Response(user)
      case Success(None) => Fail(Unauthenticated(s"Invalid email or password"))
      case Failure(exception) => Fail(exception)
    }
  }

  private def insertBlackList(token: TokenIdentifier): ServiceResponse[Boolean] = {
    blackListToken.add(token)
    Response(true)
  }
}
