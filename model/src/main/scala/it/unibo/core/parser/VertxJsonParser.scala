package it.unibo.core.parser

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, Feeder, Resource, Sensor}
import it.unibo.core.microservice.vertx._

trait VertxJsonParser extends DataParser[JsonObject]{
  override def decode(rawData: JsonObject, uri: String): Option[Data] = {
    val categoryOption = rawData.getAsString("category").filter(_ == target.name)
    val timestampOption = rawData.getAsLong("timestamp")
    val feederOption = rawData.getAsObject("feeder").flatMap(extractFeeder)
    for {
      category <- categoryOption
      timestamp <- timestampOption
      feeder <- feederOption
      data <- createDataFrom(uri, feeder, timestamp, rawData)
    } yield data
  }
  override def encode(data: Data): Option[JsonObject] = {
    val obj = new JsonObject()
    if(data.category != target) {
      None
    } else {
      encodeStrategy(data.value).map {
        obj => obj
          .put("category", data.category.name)
          .put("timestamp", data.timestamp)
          .put("uri", data.URI)
          .put("feeder", encodeFeeder(data.feeder))
      }
    }
  }


  protected def createDataFrom(uri : String, feeder : Feeder, timestamp : Long, json: JsonObject) : Option[Data]

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
}
