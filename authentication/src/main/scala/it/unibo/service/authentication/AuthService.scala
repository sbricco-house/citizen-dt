package it.unibo.service.authentication

import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.MissingResource

import scala.concurrent.Future

trait SystemUser {
  def identifier: String
  def role: String
}

object SystemUser {
  private case class SystemUserImpl(identifier: String, role: String) extends SystemUser
  def apply(identifier: String, role: String): SystemUser = SystemUserImpl(identifier, role)
  def apply(idRole: (String,String)): SystemUser = SystemUserImpl(idRole._1, idRole._2)
}

trait AuthService {
  def getAuthenticatedUser(identifier: String) : FutureService[SystemUser]
}

object AuthService {
  def apply(users: Seq[SystemUser]) : AuthService =
    new Mock(users.map(u => (u.identifier, u)).toMap)

  private class Mock (users: Map[String, SystemUser]) extends AuthService {
    override def getAuthenticatedUser(identifier: String): FutureService[SystemUser] = {
      users.get(identifier)
        .map(FutureService.response)
        .getOrElse(FutureService.fail(MissingResource(s"User $identifier not found")))
    }
  }
}