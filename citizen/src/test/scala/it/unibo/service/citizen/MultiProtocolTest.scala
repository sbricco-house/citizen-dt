package it.unibo.service.citizen

import java.util.UUID

import io.vertx.lang.scala.json.Json
import io.vertx.scala.core.http.{HttpClient, WebSocketBase}
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.core.data.{Data, Resource}
import it.unibo.service.citizen.HttpScope.{CITIZEN_AUTHORIZED_HEADER, STATE_ENDPOINT, wsOptions}
import it.unibo.service.citizen.matcher.DataJsonMatcher
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.Future
import scala.util.Random

import HttpScope._
import CoapScope._

class MultiProtocolTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers with ScalaFutures with DataJsonMatcher {
  val random = new Random()
  var httpClient : WebClient = _
  var websocketClient : HttpClient = _
  "citizen service" should "work with multiple protocols works together" in {
    val httpData = randomData(50)
    val websocketData = randomData(100)
    val httpFuture = sendViaHttp(httpData)
    val websocket = createWebsocket()
  }

  def sendViaHttp(data : Seq[String]) = {
    val array = Json.arr(data:_*)
    val obj = Json.obj("data" -> array)
    httpClient.patch(HttpScope.STATE_ENDPOINT)
      .putHeader(HttpScope.CITIZEN_AUTHORIZED_HEADER)
      .sendJsonObjectFuture(obj)
  }

  def createWebsocket() : Future[WebSocketBase] = {
    websocketClient.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))
  }

  private def randomData(howMuch : Int) = {
    (0 to howMuch).map {
      case i if i % 2 == 0 => Categories.bloodPressureCategory
      case _ => Categories.heartBeatCategory
    }
      .map(Data(UUID.randomUUID().toString, Resource("_"), _, random.nextInt(), random.nextInt()))
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
      .map(_.encode())
  }

  override def beforeEach(): Unit = {
    HttpScope.boot()
    CoapScope.boot()
    httpClient = HttpScope.webClient()
    websocketClient = HttpScope.httpClient()
  }

  override def afterEach(): Unit = {
    CoapScope.teardown()
    HttpScope.teardown()
  }
}
