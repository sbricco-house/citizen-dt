package it.unibo.service.citizen

import java.net.URI

import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{HttpClient, HttpClientOptions, WebSocketConnectOptions}
import io.vertx.scala.ext.web.client.{HttpRequest, WebClient, WebClientOptions}
import it.unibo.service.citizen.websocket.WebSocketCitizenApi

import scala.concurrent.Await
import scala.concurrent.duration._

object HttpScope {
  val STATE_ENDPOINT = s"http://localhost:8080/citizens/50/state"
  val HISTORY_ENDPOINT = s"http://localhost:8080/citizens/50/history"
  val CITIZEN_AUTHORIZED_HEADER = "Authorization" -> "jwt1"
  val STAKEHOLDER_AUTHENTICATED_HEADER = "Authorization" -> "jwt2"
  val STAKEHOLDER_AUTHORIZED_HEADER = "Authorization" -> "jwt3"
  val HISTORY_LIMIT = 5
  val HEARTHBEAT_CATEGORY = "heartbeat"
  val UNKNOWN = "_"
  val HISTORY_GROUP_CATEGORY = "medical"

  var vertx: Vertx = _

  def boot(): Unit = {
    vertx = Vertx.vertx()
    val citizenVerticle = new RestCitizenVerticle(
      CitizenMicroservices.injectVertx(vertx),
      CitizenMicroservices.parserRegistry,
    ) with RestCitizenApi with WebSocketCitizenApi

    Await.result(vertx.deployVerticleFuture(citizenVerticle), Duration.Inf)
  }

  def webClient(): WebClient = {
    WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  }

  def httpClient(): HttpClient = {
    Vertx.vertx().createHttpClient(HttpClientOptions().setDefaultPort(8080))
  }

  def teardown(): Unit = {
    Await.result(vertx.closeFuture(), Duration.Inf)
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