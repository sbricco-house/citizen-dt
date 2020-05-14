package it.unibo.service.authentication

import scala.concurrent.Future

trait SystemUser {
  def identifier: String
  def role: String
}

trait AuthService {
  def getAuthenticatedUser(identifier: String) : Future[SystemUser]
}

object AuthService {
  def apply() : AuthService = new Mock()

  private class Mock extends AuthService {
    override def getAuthenticatedUser(identifier: String): Future[SystemUser] = {
      if(identifier == MockUser.identifier)
        Future.successful(MockUser)
      else
        Future.failed(new Exception())
    }
  }
  case object MockUser extends SystemUser {
    override def role: String = "citizen"
    override def identifier: String = "50"
  }
}