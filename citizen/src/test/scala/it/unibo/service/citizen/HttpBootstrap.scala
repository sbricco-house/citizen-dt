package it.unibo.service.citizen

import java.net.URI

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{HttpClient, HttpClientOptions, WebSocketConnectOptions}
import io.vertx.scala.ext.web.client.{HttpRequest, WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data._
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.core.registry.DataCategoryRegistry
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.authentication.MockAuthenticationClient
import it.unibo.service.permission.MockAuthorization

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object HttpBootstrap {
  val STATE_ENDPOINT = s"http://localhost:8080/citizens/50/state"
  val HISTORY_ENDPOINT = s"http://localhost:8080/citizens/50/history"
  val CITIZEN_AUTHORIZED_HEADER = "Authorization" -> "jwt1"
  val STAKEHOLDER_AUTHENTICATED_HEADER = "Authorization" -> "jwt2"
  val STAKEHOLDER_AUTHORIZED_HEADER = "Authorization" -> "jwt3"
  val HISTORY_LIMIT = 5
  val HISTORY_CATEGORY = "heartbeat"
  val HISTORY_GROUP_CATEGORY = "medical"

  var vertx: Vertx = _

  def boot(): Unit = {
    vertx = Vertx.vertx()

    val categoriesRegistry = DataCategoryRegistry()
    categoriesRegistry.register(Categories.medicalDataCategory)
    categoriesRegistry.register(Categories.bloodPressureCategory)

    val authenticationService = MockAuthenticationClient(Seq(
      TokenIdentifier("jwt1") -> MockSystemUser("pippo", "50", "citizen"),
      TokenIdentifier("jwt2") -> MockSystemUser("pluto", "47", "stakeholder"),
      TokenIdentifier("jwt3") -> MockSystemUser("paperino", "46", "doctor")
    ))

    val authorizationService = MockAuthorization(Map(
      ("50", "50") -> Seq(Categories.medicalDataCategory),
      ("46", "50") -> Seq(Categories.medicalDataCategory, Categories.bloodPressureCategory)
    ))

    val store = InMemoryStorage[Data, String]()
    val citizenService = CitizenService(authenticationService, authorizationService, store)

    val parser = DataParserRegistry(new Categories.HearBeatParser(), new Categories.BloodPressureParser())
    val citizenVerticle = new RestCitizenVerticle(
      citizenService,
      parser,
      categoriesRegistry,
      "50"
    ) with RestCitizenApi with WebSocketCitizenApi

    Await.result(vertx.deployVerticleFuture(citizenVerticle), 5 seconds)
  }

  def webClient(): WebClient = {
    WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  }

  def httpClient(): HttpClient = {
    Vertx.vertx().createHttpClient(HttpClientOptions().setDefaultPort(8080))
  }

  def teardown(): Unit = {
    Await.result(vertx.closeFuture(), 5 seconds)
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(value: (String, String)): HttpRequest[T] = request.putHeader(value._1, value._2)
  }

  def wsOptions(uri : String) : WebSocketConnectOptions = {
    val uriObj = URI.create(uri)
    WebSocketConnectOptions().setHost(uriObj.getHost).setPort(uriObj.getPort).setURI(uriObj.getPath)
  }

  implicit class RichWebsocketOptions[T](request: WebSocketConnectOptions) {
    def putHeader(value: (String, String)): WebSocketConnectOptions = request.addHeader(value._1, value._2)
  }

}

object MockSystemUser {
  def apply(name: String, identifier: String, role: String): SystemUser = SystemUser("", name, "", identifier, role)
}

object Categories {
  val hearBeatCategory = LeafCategory("heartbeat", 100)
  val bloodPressureCategory = LeafCategory("blood_pressure", 100)
  val medicalDataCategory = GroupCategory("medical", Set(hearBeatCategory, bloodPressureCategory))

  case class HeartBeatData(identifier: String, timestamp: Long, value: Double, feeder: Feeder) extends Data {
    override def category: LeafCategory = Categories.hearBeatCategory
  }
  class HearBeatParser extends VertxJsonParser {
    override protected def createDataFrom(identifier: String, feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => HeartBeatData(identifier, timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Double =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = hearBeatCategory
  }

  case class BloodPressureData(identifier: String, timestamp: Long, value: Int, feeder: Feeder) extends Data {
    override def category: LeafCategory = Categories.bloodPressureCategory
  }
  class BloodPressureParser extends VertxJsonParser {
    override protected def createDataFrom(identifier: String, feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => BloodPressureData(identifier, timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Int =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = Categories.bloodPressureCategory
  }
}
