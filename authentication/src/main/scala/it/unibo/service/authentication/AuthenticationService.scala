package it.unibo.service.authentication

import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.FutureService

trait AuthenticationService {
  def login(email: String, password: String): FutureService[TokenIdentifier]
  def refresh(identifier: TokenIdentifier): FutureService[TokenIdentifier]
  def logout(identifier: TokenIdentifier): FutureService[Boolean]
  def verifyToken(identifier: TokenIdentifier) : FutureService[SystemUser]
}