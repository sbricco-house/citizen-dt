package it.unibo.service.citizen

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.{Vertx, VertxOptions}
import io.vertx.scala.ext.web.client.{HttpRequest, WebClient, WebClientOptions}
import it.unibo.core.data._
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.service.authentication.{AuthService, SystemUser}
import it.unibo.service.citizen.authorization.MockAuthorization

import scala.concurrent.Future

object RestBootstrap {
  val STATE_ENDPOINT = s"http://localhost:8080/citizens/50/state"
  val HISTORY_ENDPOINT = s"http://localhost:8080/citizens/50/history"
  val CITIZEN_AUTHORIZED_HEADER = "Authorization" -> "50"
  val STAKEHOLDER_AUTHENTICATED_HEADER = "Authorization" -> "47"
  val STAKEHOLDER_AUTHORIZED_HEADER = "Authorization" -> "46"
  val HISTORY_LIMIT = 5
  val HISTORY_CATEGORY = "heartbeat"

  def boot(): Future[String] = {
    val vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(3))

    val authorizationService = AuthService(Seq(
      SystemUser("50" -> "citizen"),
      SystemUser("47" -> "stakeholder"),
      SystemUser("46" -> "doctor")
    ))
    val authorizationFacade = MockAuthorization(Map(
      ("50", "50") -> Seq(HeartBeat.medicalDataCategory),
      ("46", "50") -> Seq(HeartBeat.medicalDataCategory)
    ))

    val store = InMemoryStorage[Data, String]()
    val citizenService = CitizenService(authorizationFacade, store)

    val parser = DataParserRegistry(new HeartBeat.HearBeatParser())
    vertx.deployVerticleFuture(new RestCitizenVerticle(authorizationService, citizenService, parser, "50"))
  }

  def webClient(): WebClient = {
    val vertx = Vertx.vertx()
    WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(value: (String, String)): HttpRequest[T] = request.putHeader(value._1, value._2)
  }
}

object HeartBeat {
  val category: LeafCategory = LeafCategory("heartbeat", 100)
  val medicalDataCategory = GroupCategory("medical", Set(category))

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
