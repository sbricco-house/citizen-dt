package it.unibo.service.authentication

import java.util.concurrent.Executors

import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.auth.User
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse, _}
import it.unibo.core.utils.ServiceError.Unauthenticated
import it.unibo.service.authentication.utils.Hash

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import AuthenticationServiceBackend._
import it.unibo.service.authentication.model.Parsers
import it.unibo.service.authentication.model.Resources.AuthenticationInfo

object AuthenticationServiceBackend {
  /**
   * Default token expiration time (1 day).
   */
  val EXPIRE_TIME_MIN: Int = 60 * 24
}

/**
 * Implementation of backend Authentication Service based on Vertx's JWT Token.
 * It provides the logout functionality using a blacklist system.
 * In real case scenario this implementation can be improved using Refresh token and Access token.
 *
 * @param provider Vertx JWT Token provider, for generate and check the jwt tokens.
 * @param userStorage Storage that contains the system users. Used for check the right login credentials.
 */
class AuthenticationServiceBackend(provider: JWTAuth,
                                   userStorage: Storage[SystemUser, String]) extends AuthenticationService {

  private implicit val context: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  private val blackListToken: mutable.LinkedHashSet[TokenIdentifier] = mutable.LinkedHashSet()
  private val jwtOptions = JWTOptions()
    .setExpiresInMinutes(EXPIRE_TIME_MIN)

  override def login(email: String, password: String): FutureService[AuthenticationInfo] = {
    FutureService(loginUser(email, Hash.SHA256.digest(password)))
      .map(user => AuthenticationInfo(generateToken(user), user))
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

  override def refresh(identifier: TokenIdentifier): FutureService[Token] = {
    // authenticate the user, add current token to blacklist, regenerate the token
    verifyToken(identifier)
      .map(user => {
        insertBlackList(identifier)
        generateToken(user)
      })
  }

  private def generateToken(user: SystemUser): Token = {
    // introduce fixed delay for prevent creation of same jwt. By definition JWT time is expressed in seconds
    // generating a token for the same user at high rate < 1s could generate same token. In real scenario
    // this not happens, but is better to prevent this
    Thread.sleep(1000)
    val token = provider.generateToken(userToClaims(user), jwtOptions)
    Token(token, EXPIRE_TIME_MIN)
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

  // Internal payload (claims) of vertx JWT
  protected def claimsToUser(user: JsonObject): SystemUser = Parsers.SystemUserParser.decode(user).get
  protected def userToClaims(user: SystemUser): JsonObject = Parsers.SystemUserParser.encode(user)

}
