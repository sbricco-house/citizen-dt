package it.unibo.service.authentication

import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.Executors

import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.auth.User
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse, _}
import it.unibo.core.utils.ServiceError.Unauthenticated

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class BackendAuthenticationService(provider: JWTAuth,
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

  override def verifyToken(identifier: TokenIdentifier): FutureService[SystemUser] = {
    if(blackListToken contains identifier) {
      FutureService.fail(Unauthenticated(s"Invalid or expired token"))
    } else {
      authenticateToken(identifier).map(decoded => claimsToUser(decoded.principal()))
    }
  }

  private def authenticateToken(identifier: TokenIdentifier): FutureService[User] = {
    provider.authenticateFuture(new JsonObject().put("jwt", identifier.token))
      .transformToFutureService {
        case Failure(_) => Fail(Unauthenticated(s"Invalid or expired token"))
        case Success(value) => Response(value)
      }
  }

  override def logout(identifier: TokenIdentifier): FutureService[Boolean] = {
    verifyToken(identifier)
      .flatMap(_ => FutureService(insertBlackList(identifier)))
  }

  override def refresh(identifier: TokenIdentifier): FutureService[TokenIdentifier] = {
    // authenticate the user, add current token to blacklist, regenerate the token
    verifyToken(identifier)
      .map(user => {
        insertBlackList(identifier)
        generateToken(user)
      })
  }

  private def generateToken(user: SystemUser): TokenIdentifier = {
    // introduce fixed delay for prevent creation of same jwt. By definition JWT time is expressed in seconds
    // generating a token for the same user at high rate < 1s could generate same token. In real scenario
    // this not happens, but is better to prevent this
    Thread.sleep(1000)
    val token = provider.generateToken(userToClaims(user), jwtOptions)
    TokenIdentifier(token)
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

  protected def claimsToUser(user: JsonObject): SystemUser = {
    SystemUser(
      user.getString("email"),
      user.getString("username"),
      "", // no password,
      user.getString("identifier"),
      user.getString("role")
    )
  }

  protected def userToClaims(user: SystemUser): JsonObject = {
    new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
  }
}
