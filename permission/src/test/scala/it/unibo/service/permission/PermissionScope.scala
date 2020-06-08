package it.unibo.service.permission

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.data.LeafCategory
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}

object PermissionScope {
  val categoryA = LeafCategory("A")
  val categoryB = LeafCategory("B")
  val categoryC = LeafCategory("C")
  val categoryParser = DataParserRegistry[JsonObject]()
    .registerParser(VertxJsonParser(ValueParser.Json.intParser, categoryA, categoryB, categoryC))
  val userA = TokenIdentifier("A")
  val userB = TokenIdentifier("B")
  val userC = TokenIdentifier("C")
}
