package it.unibo.service.citizen

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.core.http.{HttpClient, WebSocketBase}
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.core.data.{Data, Resource}
import it.unibo.service.citizen.HttpScope.{CITIZEN_AUTHORIZED_HEADER, STATE_ENDPOINT, wsOptions}
import it.unibo.service.citizen.matcher.DataJsonMatcher
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{Future, Promise}
import scala.util.Random
import it.unibo.core.microservice.protocol.{WebsocketRequest, WebsocketUpdate}
import it.unibo.service.citizen.CitizenWebsocketApiTest.{bloodPressureData, hearbeatData}
import it.unibo.service.citizen.websocket.CitizenProtocol
import org.scalatest.time.{Millis, Seconds, Span}
import HttpScope._
import CoapScope._
import CitizenMicroservices._
class MultiProtocolTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(15, Seconds), interval = Span(100, Millis))
  var timestamp = 0
  val random = new Random()
  var httpClient : WebClient = _
  var websocketClient : HttpClient = _
  "citizen service" should "work with multiple protocols works together" in {
    val httpData = randomData(50)
    val websocketData = randomData(100)
    val allData = (httpData ++ websocketData).sortBy(_.getLong("timestamp"))
    val howMany = allData.size
    val coapClient = CoapScope.createClientByCategory(Categories.medicalDataCategory)
    val websocketFuture = createWebsocket()

    whenReady(websocketFuture){
      websocket => {
        val coapPromise = CoapScope.installExpectedMany(coapClient, howMany)
        val websocketPromise = installObservingOnWebsocket(websocket, howMany)
        val httpFuture = sendViaHttp(httpData)
        val websocketParsed = websocketData.zipWithIndex
          .map { case (data, index) => WebsocketRequest[JsonArray](index, Json.arr(data)) }
          .map(CitizenProtocol.requestParser.decode)
        websocketParsed.foreach(websocket.writeTextMessage)

        whenReady(coapPromise.future) {
          result => {
            val decodedResult = result.map(Json.fromObjectString).toSeq.sortBy(_.getLong("timestamp"))
            decodedResult.zip(allData).foreach { case (left, right) => left should sameData(right) }
          }
        }
        whenReady(websocketPromise.future) {
          result => {
            val decodedResult = result.map(Json.fromObjectString).toSeq.sortBy(_.getLong("timestamp"))
            decodedResult.zip(allData).foreach { case (left, right) => right should sameData(left) }
          }
        }
      }
    }
  }

  def sendViaHttp(data : Seq[JsonObject]) = {
    val array = Json.arr(data:_*)
    val obj = Json.obj("data" -> array)
    httpClient.patch(HttpScope.STATE_ENDPOINT)
      .putHeader(HttpScope.CITIZEN_AUTHORIZED_HEADER)
      .sendJsonObjectFuture(obj)
  }

  private def createWebsocket() : Future[WebSocketBase] = {
    websocketClient.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))
  }

  private def installObservingOnWebsocket(websocket : WebSocketBase, howMany : Int) : Promise[Set[String]] = {
    val promise = Promise[Set[String]]
    var elements = Set.empty[String]
    websocket.textMessageHandler(text => {
      val response = CitizenProtocol.updateParser.encode(text)
      response match {
        case Some(update) =>
          elements += update.value.encode()
          if(elements.size == howMany) {
            promise.success(elements)
          }
        case _ =>
      }
    })
    promise
  }

  private def randomData(howMuch : Int) = {
    (0 until howMuch).map {
      case i if i % 2 == 0 => Categories.bloodPressureCategory
      case _ => Categories.heartBeatCategory
    }
      .map(Data(UUID.randomUUID().toString, Resource("_"), _, { timestamp += 1; timestamp }, random.nextInt()))
      .map(CitizenMicroservices.parserRegistry.encode)
      .map(_.get)
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
