package it.unibo.service.citizen
import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.core.http.{HttpClient, WebSocket}
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.core.microservice.protocol.{WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.service.citizen.HttpScope.{STATE_ENDPOINT, _}
import it.unibo.service.citizen.matcher.DataJsonMatcher
import it.unibo.service.citizen.websocket.{CitizenProtocol, Ok, Status}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{Future, Promise}

class CitizenWebsocketApiTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  import CitizenWebsocketApiTest._

  var client : HttpClient = _
  var webClient : WebClient = _
  "citizen api" should "can't upgrade to websocket if authorization header missing" in {
    val websocket = client.webSocketFuture(STATE_ENDPOINT)
    whenReady(websocket.failed) {
      case e => succeed
    }
  }

  "citizen api" should "enable to upgrade to websocket" in {
    val websocket = client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))
    whenReady(websocket) {
      socket => socket.close(); succeed
    }
  }
  "citizen api" should "enable to update citizen state via websocket" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(hearbeatData))
        val stringRequest = CitizenProtocol.requestParser.encode(request)
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
        val stringRequest = CitizenProtocol.requestParser.encode(request)
        channel.writeTextMessage(stringRequest)
        val update = awaitUpdate(channel)
        whenReady(update) {
          case WebsocketUpdate(data) => data should sameData(hearbeatData)
        }
        channel.close()
    }
  }

  "citizen client" should "enable to notified from citizen state rest update " in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val update = awaitUpdate(channel)
        val patchFuture = webClient.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(postData)
        whenReady(update.zip(patchFuture)) {
          case (WebsocketUpdate(data), _) => data should sameData(bloodPressureData)
        }
        channel.close()
    }
  }
  "citizen api" should "update citizen state with multiple data" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(hearbeatData, bloodPressureData))
        val stringRequest = CitizenProtocol.requestParser.encode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, Ok) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }

  "citizen service" should " manage multiple update" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel => {
        val request = WebsocketRequest[JsonArray](1, Json.arr(hearbeatData, bloodPressureData))
        val stringRequest = CitizenProtocol.requestParser.encode(request)
        val restPath = webClient.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(Useful.postState)
        channel.writeTextMessage(stringRequest)
        val awaitUpdate = awaitResponse(1, channel)
        whenReady(restPath.zip(awaitUpdate)) { result => {}}
        whenReady(webClient.get(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendFuture()) {
          result =>
            result.statusCode() shouldBe 200
            result.bodyAsJsonObject().get.getJsonArray("data") should sameArrayData(Json.arr(hearbeatData, bloodPressureData))
        }
        channel.close()
      }
    }
  }

  "citizen api" should "NOT update citizen state with wrong category" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(unkwonCategoryData))
        val stringRequest = CitizenProtocol.requestParser.encode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, CitizenProtocol.`unknownDataCategoryError`) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }
  "citizen api" should "NOT update citizen state with one wrong category at least" in {
    whenReady(client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER))) {
      channel =>
        val request = WebsocketRequest[JsonArray](1, Json.arr(unkwonCategoryData, hearbeatData, bloodPressureData))
        val stringRequest = CitizenProtocol.requestParser.encode(request)
        channel.writeTextMessage(stringRequest)
        whenReady(awaitResponse(1, channel)) {
          case WebsocketResponse(_, CitizenProtocol.`unknownDataCategoryError`) => succeed
          case _ => fail()
        }
        channel.close()
    }
  }

  override def beforeEach(): Unit = {
    CitizenMicroservices.refresh()
    HttpScope.boot()
    client = HttpScope.httpClient()
    webClient = HttpScope.webClient()
  }

  override def afterEach(): Unit = {
    HttpScope.teardown()
    httpClient.close()
    webClient.close()
  }
}
object CitizenWebsocketApiTest {
  val hearbeatData = Json.fromObjectString(
    """
      |{
      |    "id":"",
      |    "value": 90.0,
      |    "category": "heartbeat",
      |    "timestamp": 134034640,
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

  val postData = Json.obj("data" -> Json.arr(bloodPressureData))
  def awaitResponse(id : Int, websocket : WebSocket) : Future[WebsocketResponse[Status]] = {
    val promise = Promise[WebsocketResponse[Status]]()
    websocket.textMessageHandler(text => {
      val response = CitizenProtocol.responseParser.decode(text)
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
      val response = CitizenProtocol.updateParser.decode(text)
      response match {
        case Some(update) => promise.success(update)
        case _ =>
      }
    })
    promise.future
  }
}
