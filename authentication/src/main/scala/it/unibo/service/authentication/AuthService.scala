package it.unibo.service.authentication

import it.unibo.core.microservice.FutureService

trait SystemUser {
  def identifier: String
  def role: String
}

trait AuthService {
  def getAuthenticatedUser(identifier: String) : FutureService[SystemUser]
}

object SystemUser {
  private case class SystemUserImpl(identifier: String, role: String) extends SystemUser
  def apply(identifier: String, role: String): SystemUser = SystemUserImpl(identifier, role)
  def apply(idRole: (String,String)): SystemUser = SystemUserImpl(idRole._1, idRole._2)
}