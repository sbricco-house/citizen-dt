package it.unibo.core.authentication

object Resources {
  case class AuthenticationInfo(token: Token, user: SystemUser)
}
