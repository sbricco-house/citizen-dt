package it.unibo.client.demo

import it.unibo.core.authentication.{SystemUser, TokenIdentifier}

trait AuthUserProvider {
  def currentToken() : TokenIdentifier
  def currentUser(): SystemUser
}

