package it.unibo.service

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import it.unibo.core.data.{Data, Feeder, InMemoryStorage, LeafCategory, Sensor}
import it.unibo.core.dt.State
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.service.citizen.CitizenVerticle
import it.unibo.service.citizen.controller.DefaultCitizenController
import it.unibo.core.microservice.vertx._
import it.unibo.service.HeartBeat.HeartBeatData
import it.unibo.service.citizen.authorization.MockAuthorization

object CitizenApp extends App {
  val vertx = Vertx.vertx()

  var parser = DataParserRegistry(new HeartBeat.HearBeatParser())
  val store = InMemoryStorage[Data, String]()
  val authorizationFacade = MockAuthorization(HeartBeat.category)

  val state = State.empty.update(HeartBeatData(UUID.randomUUID(), 1232343L, 20, Sensor("mi_band_4")))
  val controller = new DefaultCitizenController(authorizationFacade, store, parser, state)
  val citizenVerticle = new CitizenVerticle(controller, "50")
  vertx.deployVerticle(citizenVerticle)
}

object HeartBeat {

  val category: LeafCategory = LeafCategory("heartbeat", 100)

  case class HeartBeatData(id: UUID, timestamp: Long, value: Double, feeder: Feeder) extends Data {
    override def category: LeafCategory = HeartBeat.category
  }

  class HearBeatParser extends VertxJsonParser {
    override protected def createDataFrom(id:String, feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => HeartBeatData(UUID.fromString(id), timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Double =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = category
  }
}