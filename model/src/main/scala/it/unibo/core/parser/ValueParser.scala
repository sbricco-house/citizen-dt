package it.unibo.core.parser

import io.vertx.core.json.Json
import io.vertx.lang.scala.json.{JsonObject, Json => Jsonx}
import it.unibo.core.parser.ParserLike.MismatchableParser

object ValueParser {
  /**
   * a type alias used to describe a parser that decode/encode the field value in @Data
   * @tparam External the representation of data (e.g Json, XML,..)
   */
  type ValueParser[External] = MismatchableParser[External, Any]

  /**
   * some of standard Json value parser.
   */
  object Json {
    def apply(encodeFunction: Any => Option[JsonObject])(decodeFunction : JsonObject => Option[Any]) : ValueParser[JsonObject] = ParserLike.mismatchable(encodeFunction)(decodeFunction)
    import it.unibo.core.microservice.vertx._
    implicit private def optionAnyToJson(opt : Option[Any]) : Option[JsonObject] = opt.map(data => Jsonx.obj("value"-> data))
    val intParser : ValueParser[JsonObject] = ParserLike.mismatchable[JsonObject, Any] { Some(_).filter(_.isInstanceOf[Int]) } { _.getAsInt("value") }
    val doubleParser : ValueParser[JsonObject] = ParserLike.mismatchable[JsonObject, Any] { Some(_).filter(_.isInstanceOf[Double]) } { _.getAsInt("value") }
    val stringParser : ValueParser[JsonObject] = ParserLike.mismatchable[JsonObject, Any] { Some(_).filter(_.isInstanceOf[String]) } { _.getAsString("value") }
  }
}