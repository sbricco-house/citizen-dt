package it.unibo.service.authentication

import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.FutureService

trait AuthenticationService {
  def getAuthenticatedUser(identifier: String) : FutureService[SystemUser]
}