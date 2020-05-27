package it.unibo.core.parser

import io.vertx.lang.scala.json.{Json => Jsonx, JsonObject}

trait ValueParser[Raw] extends Parser[Any, Raw] {
  override type E[O] = Option[O]
  def decode(rawData : Raw) : Option[Any]
  def encode(data : Any) : Option[Raw]
}

object ValueParser {
  //TODO do better
  object Json {
    import it.unibo.core.microservice.vertx._
    import it.unibo.macros.util.MacroUtils._

    def apply(decoder : JsonObject => Option[Any])(encoder : Any => Option[JsonObject]) : ValueParser[JsonObject] = new JsonValueParserImpl(decoder, encoder)

    private class JsonValueParserImpl(decoder : JsonObject => Option[Any], encoder : Any => Option[JsonObject]) extends ValueParser[JsonObject] {
      override def decode(rawData: JsonObject): Option[Any] = decoder(rawData)

      override def encode(data: Any): Option[JsonObject] = encoder(data)
    }

    implicit private def optionAnyToJson(opt : Option[Any]) : Option[JsonObject] = opt.map(data => Jsonx.obj("value"-> data))

    val intParser = apply { _.getAsInt("value") } { Some(_).filter(_.isInstanceOf[Int]) }
    val doubleParser = apply { _.getAsInt("value") } { Some(_).filter(_.isInstanceOf[Double]) }
    val stringParser = apply { _.getAsInt("value") } { Some(_).filter(_.isInstanceOf[String]) }
  }
}