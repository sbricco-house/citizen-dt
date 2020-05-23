package it.unibo.core.parser

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, Feeder, LeafCategory, Resource, Sensor}
import it.unibo.core.microservice.vertx._

trait VertxJsonParser extends DataParser[JsonObject]{
  def supportedCategories : Seq[LeafCategory]

  override def decode(rawData: JsonObject): Option[Data] = {
    val categoryOption = rawData.getAsString("category").flatMap(extractCategory)
    val timestampOption = rawData.getAsLong("timestamp")
    val feederOption = rawData.getAsObject("feeder").flatMap(extractFeeder)
    val idOption = rawData.getAsString("id")
    for {
      category <- categoryOption
      timestamp <- timestampOption
      feeder <- feederOption
      id <- idOption.orElse(Some(""))
      data <- createDataFrom(id, feeder, timestamp, category, rawData)
    } yield data
  }

  override def encode(data: Data): Option[JsonObject] = {
    Some(data)
      .flatMap(_ => encodeStrategy(data.value).map {
        obj => obj
          .put("category", data.category.name)
          .put("timestamp", data.timestamp)
          .put("id", data.identifier)
          .put("feeder", encodeFeeder(data.feeder))
        }
      )
  }

  protected def createDataFrom(identifier: String, feeder : Feeder, timestamp : Long, category : LeafCategory, value : JsonObject) : Option[Data]

  protected def encodeStrategy(value : Any) : Option[JsonObject]

  private def extractFeeder(jsonObject: JsonObject) : Option[Feeder] = {
    jsonObject.getAsBoolean("isResource") match {
      case None => jsonObject.getAsString("name").map(Sensor)
      case Some(true) =>jsonObject.getAsString("uri").map(Resource)
      case _ => None
    }
  }

  private def encodeFeeder(feeder: Feeder) : JsonObject = feeder match {
    case Sensor(name) => new JsonObject().put("name", name)
    case Resource(uri) => new JsonObject().put("uri", uri).put("isResource", true)
  }

  private def extractCategory(rawCategory : String) : Option[LeafCategory] = supportedCategories.find(_.name == rawCategory)

}

object VertxJsonParser {
  def apply(valueJsonParser : ValueParser[JsonObject], categories : LeafCategory *) : VertxJsonParser = new VertxJsonParser {
    override def supportedCategories: Seq[LeafCategory] = categories

    override protected def createDataFrom(identifier: String, feeder: Feeder, timestamp: Long, category: LeafCategory, value: JsonObject): Option[Data] = {
      valueJsonParser.decode(value).map(Data(identifier,feeder,category,timestamp,_))
    }

    override protected def encodeStrategy(value: Any): Option[JsonObject] = valueJsonParser.encode(value)
  }
}