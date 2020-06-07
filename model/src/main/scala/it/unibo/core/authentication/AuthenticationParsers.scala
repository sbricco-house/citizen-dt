package it.unibo.core.authentication

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.authentication.Resources.AuthenticationInfo
import it.unibo.core.parser.ParserLike.Parser
import it.unibo.core.microservice.vertx._

/**
 * A collection of main parser used by the Authentication Service Rest API.
 * It allow marshalling and unmarshalling operation for each domain concept.
 */
object AuthenticationParsers {

  /**
   *
   */
  object TokenParser extends Parser[JsonObject, Token] {
    override def encode(rawData: Token): JsonObject = new JsonObject()
      .put("token", rawData.token)
      .put("expirationInMinute", rawData.expirationInMinute)
    override def decode(data: JsonObject): Option[Token] = {
      for {
        token <- data.getAsString("token")
        expirationInMinute <- data.getAsInt("expirationInMinute")
      } yield Token(token, expirationInMinute)
    }
  }

  /**
   *
   */
  object AuthInfoParser extends Parser[JsonObject, AuthenticationInfo] {
    override def encode(rawData: AuthenticationInfo): JsonObject = new JsonObject()
      .mergeIn(TokenParser.encode(rawData.token))
      .put("user", SystemUserParser.encode(rawData.user))
    override def decode(data: JsonObject): Option[AuthenticationInfo] = for {
        token <- TokenParser.decode(data)
        userJson <- data.getAsObject("user")
        user <- SystemUserParser.decode(userJson)
      } yield AuthenticationInfo(token, user)
  }

  /**
   *
   */
  object SystemUserParser extends Parser[JsonObject, SystemUser] {
    override def encode(user: SystemUser): JsonObject = new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
    override def decode(data: JsonObject): Option[SystemUser] = for {
      email <- data.getAsString("email")
      username <- data.getAsString("username")
      identifier <- data.getAsString("identifier")
      role <- data.getAsString("role")
    } yield SystemUser(email, username, "", identifier, role)
  }
}
