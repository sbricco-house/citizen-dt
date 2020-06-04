package it.unibo.covid.data

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.data.LeafCategory
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ValueParser.ValueParser
import it.unibo.core.parser.{DataParser, DataParserRegistry, ValueParser, VertxJsonParser}
object Parsers {
  import Categories._
  val positionParser = ValueParser.Json {
    case (x : Double, y : Double) => Some(Json.obj("lat" -> x, "lng" -> y))
    case _ => None
  } {
    json => for {
      first <- json.getAsDouble("lat")
      second <- json.getAsDouble("lng")
    } yield (first, second)
  }

  val temperatureParser = ValueParser.Json {
    case (x : Double, y : String) => Some(Json.obj("val" -> x, "um" -> y))
    case _ => None
  } {
    json => for {
      temperature <- json.getAsDouble("val")
      unitOfMeasure <- json.getAsString("um")
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
    .registerParser(VertxJsonParser(ValueParser.Json.doubleParser, heartbeatCategory, bloodOxygenCategory))
    .registerParser(VertxJsonParser(seqStringParser, medicalRecordCategory))
    .registerParser(VertxJsonParser(seqStringParser, bodyTemperatureCategory))
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
