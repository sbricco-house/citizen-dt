package it.unibo.service.authentication

import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.FutureService

trait AuthenticationService {
  def login(email: String, password: String): FutureService[TokenIdentifier]
  def getAuthenticatedUser(identifier: TokenIdentifier) : FutureService[SystemUser]
  def refresh(authenticated: SystemUser): FutureService[SystemUser]
  def logout(identifier: TokenIdentifier): FutureService[Boolean]
}