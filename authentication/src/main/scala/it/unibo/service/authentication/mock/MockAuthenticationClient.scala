package it.unibo.service.authentication.mock

import it.unibo.core.authentication.{Resources, SystemUser, Token, TokenIdentifier}
import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.MissingResource
import it.unibo.service.authentication.AuthenticationService

object MockAuthenticationClient {
  def apply(users: Seq[(TokenIdentifier, SystemUser)]) : AuthenticationService =
    new MockAuthenticationClient(users.map(u => (u._1, u._2)).toMap)

  private class MockAuthenticationClient(users: Map[TokenIdentifier, SystemUser]) extends AuthenticationService {
    override def login(email: String, password: String): FutureService[Resources.AuthenticationInfo] = {
      users.find { case (_, user) => user.email == email && user.password == password }
        .map { case (token, user) => FutureService.response(Resources.AuthenticationInfo(Token(token.token, 30), user)) }
        .getOrElse(FutureService.fail(MissingResource(s"User not found")))
    }

    override def refresh(identifier: TokenIdentifier): FutureService[Token] = ???

    override def logout(identifier: TokenIdentifier): FutureService[Boolean] = ???

    override def verifyToken(identifier: TokenIdentifier): FutureService[SystemUser] = {
      users.get(identifier)
        .map(FutureService.response)
        .getOrElse(FutureService.fail(MissingResource(s"User $identifier not found")))
    }
  }
}
