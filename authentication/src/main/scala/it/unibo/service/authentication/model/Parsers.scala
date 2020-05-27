package it.unibo.service.authentication.model

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.authentication.SystemUser
import it.unibo.core.parser.Parser
import it.unibo.service.authentication.Token
import it.unibo.service.authentication.model.Resources.AuthenticationInfo
import it.unibo.core.microservice.vertx._

object Parsers {

  object TokenParser extends Parser[JsonObject, Token] {
    override type E[O] = JsonObject
    override def decode(rawData: Token): JsonObject = new JsonObject()
      .put("token", rawData.token)
      .put("expirationInMinute", rawData.expirationInMinute)
    override def encode(data: JsonObject): Option[Token] = {
      for {
        token <- data.getAsString("token")
        expirationInMinute <- data.getAsInt("expirationInMinute")
      } yield Token(token, expirationInMinute)
    }
  }

  object AuthInfoParser extends Parser[JsonObject, AuthenticationInfo] {
    override type E[O] = JsonObject
    override def decode(rawData: AuthenticationInfo): JsonObject = new JsonObject()
      .mergeIn(TokenParser.decode(rawData.token))
      .put("user", SystemUserParser.decode(rawData.user))
    override def encode(data: JsonObject): Option[AuthenticationInfo] = for {
        token <- TokenParser.encode(data)
        userJson <- data.getAsObject("user")
        user <- SystemUserParser.encode(userJson)
      } yield AuthenticationInfo(token, user)
  }

  object SystemUserParser extends Parser[JsonObject, SystemUser] {
    override type E[O] = JsonObject
    override def decode(user: SystemUser): JsonObject = new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
    override def encode(data: JsonObject): Option[SystemUser] = for {
      email <- data.getAsString("email")
      username <- data.getAsString("username")
      identifier <- data.getAsString("identifier")
      role <- data.getAsString("role")
    } yield SystemUser(email, username, "", identifier, role)
  }
}