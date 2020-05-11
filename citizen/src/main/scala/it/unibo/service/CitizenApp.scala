package it.unibo.service

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import it.unibo.core.data.{Data, Feeder, InMemoryStorage, LeafCategory, Sensor}
import it.unibo.core.dt.State
import it.unibo.core.parser.VertxJsonParser
import it.unibo.service.citizen.CitizenVerticle
import it.unibo.service.citizen.controller.{DataParserMap, DefaultCitizenController}
import it.unibo.core.microservice.vertx._
import it.unibo.service.HeartBeat.HeartBeatData

object CitizenApp extends App {
  val vertx = Vertx.vertx()

  val parser = new DataParserMap(List(new HeartBeat.HearBeatParser()))
  val store = InMemoryStorage[Data, String]()
  val controller = new DefaultCitizenController(null, store, parser)
  val citizenVerticle = new CitizenVerticle(controller, "50")
  vertx.deployVerticle(citizenVerticle)
}

object HeartBeat {

  case class HeartBeatData(id: UUID, timestamp: Long, value: Double, feeder: Feeder) extends Data {
    override def category: LeafCategory = LeafCategory("heartbeat", 100)
  }

  class HearBeatParser extends VertxJsonParser {
    override protected def createDataFrom(id:String, feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => HeartBeatData(UUID.fromString(id), timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Double =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = LeafCategory("heartbeat", 100)
  }
}