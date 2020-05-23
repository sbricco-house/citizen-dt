package it.unibo.service.citizen

import java.net.URI

import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{HttpClient, HttpClientOptions, WebSocketConnectOptions}
import io.vertx.scala.ext.web.client.{HttpRequest, WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data._
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.authentication.MockAuthenticationClient
import it.unibo.service.permission.MockAuthorization

import scala.concurrent.Await
import scala.concurrent.duration._

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
    val citizenService = CitizenService.fromVertx(authenticationService, authorizationService, store, vertx)
    val integerDataParser = VertxJsonParser(ValueParser.Json.intParser, Categories.bloodPressureCategory)
    val doubleDataParser = VertxJsonParser(ValueParser.Json.intParser, Categories.hearBeatCategory)
    val parserRegistry = DataParserRegistry()
      .registerParser(integerDataParser)
      .registerParser(doubleDataParser)
      .registerGroupCategory(Categories.medicalDataCategory)

    val citizenVerticle = new RestCitizenVerticle(
      citizenService,
      parserRegistry,
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
}
