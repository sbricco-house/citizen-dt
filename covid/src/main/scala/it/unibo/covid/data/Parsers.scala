package it.unibo.covid.data

import io.vertx.lang.scala.json.{Json, JsonObject}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}
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
}