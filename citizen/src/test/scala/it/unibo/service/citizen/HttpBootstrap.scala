package it.unibo.service.citizen

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{HttpClient, HttpClientOptions, WebSocketConnectOptions}
import io.vertx.scala.ext.web.client.{HttpRequest, WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data._
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.core.registry.DataCategoryRegistry
import it.unibo.service.citizen.authentication.MockAuthenticationClient
import it.unibo.service.permission.MockAuthorization

import scala.concurrent.Future

object HttpBootstrap {
  val STATE_ENDPOINT = s"http://localhost:8080/citizens/50/state"
  val HISTORY_ENDPOINT = s"http://localhost:8080/citizens/50/history"
  val CITIZEN_AUTHORIZED_HEADER = "Authorization" -> "50"
  val STAKEHOLDER_AUTHENTICATED_HEADER = "Authorization" -> "47"
  val STAKEHOLDER_AUTHORIZED_HEADER = "Authorization" -> "46"
  val HISTORY_LIMIT = 5
  val HISTORY_CATEGORY = "heartbeat"
  val HISTORY_GROUP_CATEGORY = "medical"

  def boot(): Future[String] = {
    val vertx = Vertx.vertx()

    val categoriesRegistry = DataCategoryRegistry()
    categoriesRegistry.register(Categories.medicalDataCategory)
    categoriesRegistry.register(Categories.bloodPressureCategory)

    val authenticationService = MockAuthenticationClient(Seq(
      SystemUser("50" -> "citizen"),
      SystemUser("47" -> "stakeholder"),
      SystemUser("46" -> "doctor")
    ))

    val authorizationService = MockAuthorization(Map(
      ("50", "50") -> Seq(Categories.medicalDataCategory),
      ("46", "50") -> Seq(Categories.medicalDataCategory, Categories.bloodPressureCategory)
    ))

    val store = InMemoryStorage[Data, String]()
    val citizenService = CitizenService(authenticationService, authorizationService, store)

    val parser = DataParserRegistry(new Categories.HearBeatParser())
    val citizenVerticle = new RestCitizenVerticle(
      citizenService,
      parser,
      categoriesRegistry,
      "50"
    ) with RestCitizenApi with WebSocketCitizenApi

    vertx.deployVerticleFuture(citizenVerticle)
  }

  def webClient(): WebClient = {
    val vertx = Vertx.vertx()
    WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  }

  def httpClient(): HttpClient = {
    Vertx.vertx().createHttpClient(HttpClientOptions().setDefaultPort(8080))
  }

  implicit class RichHttpRequest[T](request: HttpRequest[T]) {
    def putHeader(value: (String, String)): HttpRequest[T] = request.putHeader(value._1, value._2)
  }

  implicit class RichWebsocketOptions[T](request: WebSocketConnectOptions) {
    def putHeader(value: (String, String)): WebSocketConnectOptions = request.addHeader(value._1, value._2)
  }

  def wsOptions(uri : String) : WebSocketConnectOptions = WebSocketConnectOptions().setURI(uri)
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
}
