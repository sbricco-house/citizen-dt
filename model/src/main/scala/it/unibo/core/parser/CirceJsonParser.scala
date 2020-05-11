package it.unibo.core.parser

import io.circe.Json
import it.unibo.core.data.{Data, Feeder, LeafCategory, Resource, Sensor}

/**
 * JSON parser skeleton
 */
trait CirceJsonParser extends DataParser[Json]{
  import CirceJsonParser._

  override def decode(rawData: Json): Option[Data] = {
    val categoryOption = extractCategory(rawData, target)
    val feederOption = extractFeeder(rawData)
    val timestampOption = extractTimestamp(rawData)
    val idOption = rawData.hcursor.downField("id").focus.flatMap(_.asString)
    val jsonValue = rawData.hcursor.downField("value").focus
    for {
      category <- categoryOption
      feeder <- feederOption
      timestamp <- timestampOption
      id <- idOption
      value <- jsonValue
      data <- createDataFrom(id, feeder, timestamp, value)
    } yield data
  }

  override def encode(data: Data): Option[Json] = if(data.category == target) {
    val category = Json.fromString(data.category.name)
    val id = Json.fromString(data.id.toString)
    val timestamp = Json.fromLong(data.timestamp)
    val feeder = encodeFeeder(data.feeder)
    encodeStrategy(data.value).map(valueJson => {
      Json.obj(
        "category" -> category,
        "id" -> id,
        "timestamp" -> timestamp,
        "feeder" -> feeder,
        "value" -> valueJson
      )
    })
  } else {
    None
  }

  protected def createDataFrom(id:String, feeder : Feeder, timestamp : Long, json: Json) : Option[Data]

  protected def encodeStrategy(value : Any) : Option[Json]
}

object CirceJsonParser {
  private def extractCategory(json: Json, leafCategory: LeafCategory) : Option[LeafCategory] = {
    val category = json.hcursor.downField("category").focus
    category.flatMap(_.asString).filter(leafCategory.name == _).map(_ => leafCategory)
  }

  private def extractFeeder(json: Json) : Option[Feeder] = {
    //TODO put isResouce or is better to have name / uri discriminant?
    val feeder = json.hcursor.downField("feeder")
    val feederType = feeder.downField("isResource").focus
    feederType match {
      case None => feeder.downField("name").focus.flatMap(_.asString).map(name => Sensor(name))
      case Some(value) => feeder.downField("uri").focus.flatMap(_.asString).map(uri => Resource(uri))
    }
  }

  private def extractTimestamp(json : Json) : Option[Long] = {
    val timestamp = json.hcursor.downField("timestamp").focus
    timestamp.flatMap(_.asNumber).map(_.truncateToLong)
  }

  private def encodeFeeder(feeder : Feeder) : Json = feeder match {
    case Sensor(name) => Json.obj("name" -> Json.fromString(name))
    case Resource(uri) => Json.obj("uri" -> Json.fromString(uri), "isResource" -> Json.True)
  }
}
