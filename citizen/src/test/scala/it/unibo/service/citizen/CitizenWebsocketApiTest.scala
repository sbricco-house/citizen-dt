package it.unibo.service.citizen
import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.core.http.{HttpClient, WebSocket}
import it.unibo.core.microservice.protocol.{WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.service.citizen.HttpBootstrap.{STATE_ENDPOINT, _}
import it.unibo.service.citizen.matcher.DataJsonMatcher
import it.unibo.service.citizen.websocket.{CitizenProtocol, Ok, Status}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{Future, Promise}

class CitizenWebsocketApiTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  import CitizenWebsocketApiTest._

  var client : HttpClient = _
  "citizen api" should "can't upgrade to websocket if authorization header missing" in {
    val websocket = client.webSocketFuture(STATE_ENDPOINT)
    whenReady(websocket.failed) {
      case e => succeed
    }
  }

  "citizen api" should "enable to upgrade to websocket" in {
    val websocket = client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))
    whenReady(websocket) {
      case e => succeed
    }
  }
  "citizen api" should "enable to update citizen state via websocket" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(hearbeatData))
        val stringRequest = CitizenProtocol.requestParser.decode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, Ok) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }

  "citizen client" should "enable to notificated from citizen state update" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(hearbeatData))
        val stringRequest = CitizenProtocol.requestParser.decode(request)
        channel.writeTextMessage(stringRequest)
        val update = awaitUpdate(channel)
        whenReady(update) {
          case _ => succeed
        }
        channel.close()
    }
  }

  "citizen api" should "update citizen state with multiple data" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(hearbeatData, bloodPressureData))
        val stringRequest = CitizenProtocol.requestParser.decode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, Ok) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }

  "citizen api" should "NOT update citizen state with wrong category" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(unkwonCategoryData))
        val stringRequest = CitizenProtocol.requestParser.decode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, CitizenProtocol.unkwonDataCategoryError) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }
  "citizen api" should "NOT update citizen state with one wrong category at least" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(unkwonCategoryData, hearbeatData, bloodPressureData))
        val stringRequest = CitizenProtocol.requestParser.decode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, CitizenProtocol.unkwonDataCategoryError) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }

  override def beforeAll(): Unit = {
    HttpBootstrap.boot()
    client = HttpBootstrap.httpClient()
  }

  override def afterAll(): Unit = {
    HttpBootstrap.teardown()
    client.close()
  }
}
object CitizenWebsocketApiTest {
  val hearbeatData = Json.fromObjectString(
    """
      |{
      |    "id":"",
      |    "value": 75.0,
      |    "category": "heartbeat",
      |    "timestamp": 134034600,
      |    "feeder": {
      |      "name": "mi_band_3"
      |    }
      |}
      |""".stripMargin
  )

  val bloodPressureData = Json.fromObjectString(
    """
    |{
    | "id":"",
    | "value": 91,
    | "category": "blood_pressure",
    | "timestamp": 126034600,
    | "feeder": {
    |  "name": "mi_band_4"
    | }
    |}""".stripMargin
  )

  val unkwonCategoryData = Json.fromObjectString(
    """
      |{
      | "id":"",
      | "value": 91,
      | "category": "unkwon",
      | "timestamp": 126034600,
      | "feeder": {
      |  "name": "mi_band_4"
      | }
      |}""".stripMargin
  )

  def awaitResponse(id : Int, websocket : WebSocket) : Future[WebsocketResponse[Status]] = {
    val promise = Promise[WebsocketResponse[Status]]()
    websocket.textMessageHandler(text => {
      val response = CitizenProtocol.responseParser.encode(text)
      response match {
        case Some(result @ WebsocketResponse(`id`, _)) => promise.success(result)
        case _ =>
      }
    })
    promise.future
  }

  def awaitUpdate(websocket : WebSocket) : Future[WebsocketUpdate[JsonObject]] = {
    val promise = Promise[WebsocketUpdate[JsonObject]]()
    websocket.textMessageHandler(text => {
      val response = CitizenProtocol.updateParser.encode(text)
      response match {
        case Some(update) => promise.success(update)
        case _ =>
      }
    })
    promise.future
  }
}
