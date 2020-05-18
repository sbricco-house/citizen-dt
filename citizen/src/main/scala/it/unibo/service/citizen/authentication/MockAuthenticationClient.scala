package it.unibo.service.citizen.authentication

import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.MissingResource
import it.unibo.service.authentication.AuthenticationService

object MockAuthenticationClient {
  def apply(users: Seq[SystemUser]) : AuthenticationService =
    new MockAuthenticationClient(users.map(u => (u.identifier, u)).toMap)

  private class MockAuthenticationClient(users: Map[String, SystemUser]) extends AuthenticationService{
    override def getAuthenticatedUser(identifier: String): FutureService[SystemUser] = {
      users.get(identifier)
        .map(FutureService.response)
        .getOrElse(FutureService.fail(MissingResource(s"User $identifier not found")))
    }
  }
}
