package it.unibo.covid.data

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ValueParser.ValueParser
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}

/**
 * a set of a standard parsers used in covid domain.
 * Allow the creation of data registry parser (i.e. a registry in which multiple parser are installed)
 * from standard categories or from a configuration file.
 */
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
    case seq : Seq[String] => Some(Json.obj("value" -> Json.arr(seq:_*)))
    case _ => None
  } {
    json => json.getAsArray("value").flatMap(_.getAsStringSeq)
  }

  /**
   * create a standard registry that handle:
   *  - personal data
   *  - medical data
   *  - position data
   * @return the registry create
   */
  def configureRegistry() : DataParserRegistry[JsonObject] = DataParserRegistry.emptyJson
    //personal information
    .registerParser(VertxJsonParser(ValueParser.Json.stringParser, nameCategory, surnameCategory, birthdateCategory, fiscalCodeCategory))
    .registerGroupCategory(personalDataCategory)
    //medical data
    .registerParser(VertxJsonParser(ValueParser.Json.intParser, heartrateCategory))
    .registerParser(VertxJsonParser(ValueParser.Json.doubleParser, bloodOxygenCategory))
    .registerParser(VertxJsonParser(temperatureParser, bodyTemperatureCategory))
    .registerParser(VertxJsonParser(seqStringParser, medicalRecordCategory))
    .registerGroupCategory(medicalDataCategory)
    //location data
    .registerParser(VertxJsonParser(positionParser, positionCategory))
    .registerGroupCategory(locationCategory)

  /**
   * This method allow the creation of a data registry from a json array.
   * each element of the array is a json object composed by:
   * {
   *   name : #category name
   *   ttl : #time to life of the category
   *   type : "float" | "double" | "string" | "string[]" | "position" | "temperature" | ... other created by developer
   *   groups : [#array of category group]
   * }
   * @param jsonArray The categories specified in json
   * @param supportedParser The supported parser for category
   * @return the registry created
   */
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
