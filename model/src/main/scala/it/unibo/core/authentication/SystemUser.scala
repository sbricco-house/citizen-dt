package it.unibo.core.authentication

/**
 *
 */
trait SystemUser {
  def email: String
  def username: String
  def password: String
  def identifier: String
  def role: String
}

object SystemUser {
  private case class SystemUserImpl(email: String, username: String, password: String, identifier: String, role: String) extends SystemUser
  def apply(email: String, username: String, password: String, identifier: String, role: String): SystemUser =
    SystemUserImpl(email, username, password, identifier, role)
}