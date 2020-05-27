package it.unibo.service.authentication.model

import it.unibo.core.authentication.SystemUser
import it.unibo.service.authentication.Token

object Resources {
  case class AuthenticationInfo(token: Token, user: SystemUser)
}