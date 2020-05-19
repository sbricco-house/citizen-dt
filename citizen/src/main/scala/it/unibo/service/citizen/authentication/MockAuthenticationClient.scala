package it.unibo.service.citizen.authentication

import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.FutureService
import it.unibo.core.protocol.ServiceError.MissingResource
import it.unibo.service.authentication.{AuthenticationService, TokenIdentifier}

object MockAuthenticationClient {
  def apply(users: Seq[(TokenIdentifier, SystemUser)]) : AuthenticationService =
    new MockAuthenticationClient(users.map(u => (u._1, u._2)).toMap)

  private class MockAuthenticationClient(users: Map[TokenIdentifier, SystemUser]) extends AuthenticationService {
    override def login(email: String, password: String): FutureService[TokenIdentifier] = ???

    override def verifyToken(identifier: TokenIdentifier): FutureService[SystemUser] = {
      users.get(identifier)
        .map(FutureService.response)
        .getOrElse(FutureService.fail(MissingResource(s"User $identifier not found")))
    }

    override def refresh(authenticated: TokenIdentifier): FutureService[TokenIdentifier] = ???

    override def logout(identifier: TokenIdentifier): FutureService[Boolean] = ???
  }
}
