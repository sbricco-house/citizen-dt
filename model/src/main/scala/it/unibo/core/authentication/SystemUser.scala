package it.unibo.core.authentication

/**
 * the representation of an user logged in the system.
 * It has:
 *  - an email
 *  - an username used to logged in
 *  - a secret password
 *  - a global identifier
 *  - a role
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