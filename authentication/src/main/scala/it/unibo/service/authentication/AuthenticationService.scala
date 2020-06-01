package it.unibo.service.authentication


import java.net.URI

import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.client.AuthenticationClient
import it.unibo.service.authentication.model.Resources.AuthenticationInfo

/**
 * Abstraction of Authentication Service expressed using main domain concept.
 * It allow to authenticate and provide to the user a token identifier
 */
trait AuthenticationService {

  /**
   * Make a login phase using email and password as access provider.
   * @param email User's email
   * @param password User's password
   * @return Response(authInfo) if @email and @password are valid. Fail(Unauthenticated) if are not valid.
   * @see [[AuthenticationInfo]]
   */
  def login(email: String, password: String): FutureService[AuthenticationInfo]

  /**
   * Refresh the user's current token providing a new one.
   * @param identifier The current user token identifier
   * @return Response(Token) A new Token with expiration time if current token is valid. Fail(error) if is the token is expired or invalid
   * @see [[TokenIdentifier]] [[Token]]
   */
  def refresh(identifier: TokenIdentifier): FutureService[Token]

  /**
   * Log out the user using his token identifier. (Can be implemented using blacklist system or similar)
   * @param identifier User's token identifier
   * @return Response(true) if logout happens with success, Success(false) or Fail(error) if something went wrong (invalid token)
   */
  def logout(identifier: TokenIdentifier): FutureService[Boolean]

  /**
   * Check the validity of token and returns the info about the associated user. (Implementation can simply decode the token e.g. JWT)
   * @param identifier The user's token identifier
   * @return Response(user) if the token is valid. Fail(error) if token is expired or invalid
   */
  def verifyToken(identifier: TokenIdentifier) : FutureService[SystemUser]
}

object AuthenticationService {
  /**
   * Create a default authentication service implementation using token identifier as vertx's JWT.
   * @param provider Vertx JWT Token provider, for generate and check the jwt tokens.
   * @param userStorage Storage that contains the system users. Used for check the right login credentials.
   * @return AuthenticationService instance
   */
  def apply(provider: JWTAuth, userStorage: Storage[SystemUser, String]): AuthenticationService = new AuthenticationServiceBackend(provider, userStorage)

  /**
   * Create an AuthenticationService proxy client using the same interface of the service.
   * @param serviceUri Uri where the real Authentication Service is located
   * @return AuthenticationService instance
   */
  def createProxy(serviceUri: URI): AuthenticationService = AuthenticationClient(serviceUri)
}