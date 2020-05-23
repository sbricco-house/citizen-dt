package it.unibo.service.authentication


import java.net.URI

import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.Storage
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.client.AuthenticationClient

trait AuthenticationService {
  def login(email: String, password: String): FutureService[TokenIdentifier]
  def refresh(identifier: TokenIdentifier): FutureService[TokenIdentifier]
  def logout(identifier: TokenIdentifier): FutureService[Boolean]
  def verifyToken(identifier: TokenIdentifier) : FutureService[SystemUser]
}

object AuthenticationService {
  def apply(provider: JWTAuth, userStorage: Storage[SystemUser, String]): AuthenticationService = new AuthenticationServiceBackend(provider, userStorage)
  def createProxy(serviceUri: URI): AuthenticationService = AuthenticationClient(serviceUri)
}