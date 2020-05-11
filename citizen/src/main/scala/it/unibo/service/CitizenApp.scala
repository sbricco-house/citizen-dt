package it.unibo.service

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import it.unibo.core.data.{Data, Feeder, InMemoryStorage, LeafCategory, Sensor}
import it.unibo.core.dt.State
import it.unibo.core.parser.VertxJsonParser
import it.unibo.service.citizen.CitizenVerticle
import it.unibo.service.citizen.controller.DefaultCitizenController
import it.unibo.core.microservice.vertx._
import it.unibo.service.HeartBeat.HeartBeatData

object CitizenApp extends App {
  val vertx = Vertx.vertx()

  val parsers = List(new HeartBeat.HearBeatParser())
  val store = InMemoryStorage[Data, String]()
  //val state = mockState()
  val controller = new DefaultCitizenController(null, store, parsers = parsers)
  val citizenVerticle = new CitizenVerticle(controller, "50")
  vertx.deployVerticle(citizenVerticle)


  private def mockState() : State = {
    var state = State.empty
    state = state.update(HeartBeatData(13403010L, 67, Sensor("mi_band_4")))
    state
  }
}

object HeartBeat {

  case class HeartBeatData(timestamp: Long, value: Double, feeder: Feeder) extends Data {
    override def category: LeafCategory = LeafCategory("heartbeat", 100)
  }

  class HearBeatParser extends VertxJsonParser {
    override protected def createDataFrom(feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => HeartBeatData(timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Double =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = LeafCategory("heartbeat", 100)
  }
}