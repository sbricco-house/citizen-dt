package it.unibo.service.authentication

protected abstract class TokenValue(token: String)

case class TokenIdentifier(token: String) extends TokenValue(token)
case class Token(token: String, expirationInMinute: Int) extends TokenValue(token)
