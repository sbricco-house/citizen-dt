package it.unibo.covid.data

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ValueParser.ValueParser
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}
object Parsers {
  import Categories._
  val positionParser = ValueParser.Json {
    case (x : Double, y : Double) =>
      val result = Json.obj(
        "value" -> Json.obj("lat" -> x, "lng" -> y)
      )
      Some(result)
    case _ => None
  } {
    json => {
      for {
        value <- json.getAsObject("value")
        first <- value.getAsDouble("lat")
        second <- value.getAsDouble("lng")
      } yield (first, second)
    }
  }

  val temperatureParser = ValueParser.Json {
    case (x : Double, y : String) =>
      val result = Json.obj(
        "value" -> Json.obj("val" -> x, "um" -> y)
      )
      Some(result)
    case _ => None
  } {
    json => for {
      value <- json.getAsObject("value")
      temperature <- value.getAsDouble("val")
      unitOfMeasure <- value.getAsString("um")
    } yield (temperature, unitOfMeasure)
  }

  val seqStringParser = ValueParser.Json {
    case seq : Seq[String] => Some(Json.obj("elements" -> Json.arr(seq:_*)))
    case _ => None
  } {
    json => json.getAsArray("elements").flatMap(_.getAsStringSeq)
  }

  def configureRegistry() : DataParserRegistry[JsonObject] = DataParserRegistry()
    //personal information
    .registerParser(VertxJsonParser(ValueParser.Json.stringParser, nameCategory, surnameCategory, birthdateCategory, fiscalCodeCategory))
    .registerGroupCategory(personalDataCategory)
    //medical data
    .registerParser(VertxJsonParser(ValueParser.Json.doubleParser, bodyTemperatureCategory, bloodOxygenCategory))
    .registerParser(VertxJsonParser(ValueParser.Json.intParser, heartbeatCategory))
    .registerParser(VertxJsonParser(seqStringParser, medicalRecordCategory))
    .registerGroupCategory(medicalDataCategory)
    //location data
    .registerParser(VertxJsonParser(positionParser, positionCategory))
    .registerGroupCategory(locationCategory)


  def configureRegistryFromJson(jsonArray: JsonArray,
                                supportedParser: String => Option[ValueParser[JsonObject]] = parserByType) : DataParserRegistry[JsonObject] = {
    DataParserRegistryParser(supportedParser).decode(jsonArray).get
  }

  def parserByType(parserType: String): Option[ValueParser[JsonObject]] = parserType.toLowerCase match {
    case "float" | "double" => Some(ValueParser.Json.doubleParser)
    case "string" => Some(ValueParser.Json.stringParser)
    case "string[]" => Some(seqStringParser)
    case "position" => Some(positionParser)
    case "temperature" => Some(temperatureParser)
    case _ => None
  }
}
