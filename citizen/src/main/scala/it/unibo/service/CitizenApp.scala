package it.unibo.service

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import it.unibo.core.data._
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.service.citizen.authorization.MockAuthorization
import it.unibo.service.citizen.{CitizenService, RestCitizenVerticle}

object CitizenApp extends App {
  val vertx = Vertx.vertx()

  val authorizationFacade = MockAuthorization(Map(
    ("50", "50") -> Seq(HeartBeat.category)
  ))
  val store = InMemoryStorage[Data, String]()
  val citizenService = CitizenService(authorizationFacade, store)

  var parser = DataParserRegistry(new HeartBeat.HearBeatParser())
  vertx.deployVerticle(new RestCitizenVerticle(citizenService, parser, "50"))
}

object HeartBeat {

  val category: LeafCategory = LeafCategory("heartbeat", 100)

  case class HeartBeatData(identifier: String, timestamp: Long, value: Double, feeder: Feeder) extends Data {
    override def category: LeafCategory = HeartBeat.category
  }

  class HearBeatParser extends VertxJsonParser {
    override protected def createDataFrom(identifier: String, feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => HeartBeatData(identifier, timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Double =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = category
  }
}