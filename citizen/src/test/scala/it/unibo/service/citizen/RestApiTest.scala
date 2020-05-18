package it.unibo.service.citizen

import java.io.{FileOutputStream, PrintStream}
import java.util.concurrent.TimeUnit

import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.service.citizen.RestBootstrap._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class RestApiTest extends AsyncFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures with DataJsonMatcher {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  private var client: WebClient = _

  "Citizen state" should " be hidden by unauthenticated user" in {
    whenReady(client.get(STATE_ENDPOINT).sendFuture()) {
      result => result.statusCode() shouldBe 400
    }
  }

  "Citizen state" should " be update by himself" in {
    whenReady(client.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(Useful.postState)) {
      result => result.statusCode() shouldBe 201
    }
  }

  "Citizen state" should " be visible by himself" in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendFuture()) {
      result =>
        result.statusCode() shouldBe 200
        result.bodyAsJsonObject().get.getJsonArray("data") should sameArrayData(Useful.postState.getJsonArray("data"))
    }
  }

  "Citizen state" should " be hidden by unauthorized stakeholder" in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(STAKEHOLDER_AUTHENTICATED_HEADER).sendFuture()) {
      result => result.statusCode() shouldBe 403
    }
  }

  "Citizen state" should " be visible by authorized stakeholder " in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(STAKEHOLDER_AUTHORIZED_HEADER).sendFuture()) {
      result => result.statusCode() shouldBe 200
    }
  }

  "Data published " should " be of supported category" in {
    whenReady(client.patch(STATE_ENDPOINT)
      .putHeader(CITIZEN_AUTHORIZED_HEADER)
      .sendJsonObjectFuture(Useful.postStateInvalidCategory))
    {
      result => result.statusCode() shouldBe 400
    }
  }

  "Data published" should " be of LeafCategory only" in {
    whenReady(client.patch(STATE_ENDPOINT)
      .putHeader(CITIZEN_AUTHORIZED_HEADER)
      .sendJsonObjectFuture(Useful.postDataGroupCategory))
    {
      result => result.statusCode() shouldBe 400
    }
  }

  "History" should " be readable from citizen" in {
    whenReady(client.get(HISTORY_ENDPOINT)
      .putHeader(CITIZEN_AUTHORIZED_HEADER)
      .addQueryParam("data_category", HISTORY_CATEGORY)
      .addQueryParam("limit", HISTORY_LIMIT.toString).sendFuture())
    {
      result =>
        result.statusCode() shouldBe 200
        result.bodyAsJsonArray().get should sameArrayData(Useful.postState.getJsonArray("data"))
    }
  }

  "History" should " read with a group of category" in {
    whenReady(client.get(HISTORY_ENDPOINT)
      .putHeader(CITIZEN_AUTHORIZED_HEADER)
      .addQueryParam("data_category", HISTORY_GROUP_CATEGORY)
      .addQueryParam("limit", HISTORY_LIMIT.toString).sendFuture())
    {
      result =>
        result.statusCode() shouldBe 200
        result.bodyAsJsonArray().get should sameArrayData(Useful.postState.getJsonArray("data"))
    }
  }

  "History" should " be readable only for a valid data category" in {
    whenReady(client.get(HISTORY_ENDPOINT)
      .putHeader(CITIZEN_AUTHORIZED_HEADER)
      .addQueryParam("data_category", "not_existing")
      .addQueryParam("limit", "1").sendFuture())
    {
        result => result.statusCode() shouldBe 400
    }
  }

  "History" should " readable by a stakeholder" in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(STAKEHOLDER_AUTHORIZED_HEADER).sendFuture()) {
      result => result.statusCode() shouldBe 200
    }
  }

  "History" should " be hidden by a unauthorized stakeholder" in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(STAKEHOLDER_AUTHENTICATED_HEADER).sendFuture()) {
      result => result.statusCode() shouldBe 403
    }
  }

  "Single history data " should " readable from citizen" in {
    whenReady(client.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(Useful.postState)) {
      result =>
        result.statusCode() shouldBe 201
        val response = result.bodyAsJsonObject().get
        val dataIdentifier = response.getJsonArray("data").getJsonObject(0).getString("id")
        whenReady(client.get(s"$HISTORY_ENDPOINT/$dataIdentifier").putHeader(CITIZEN_AUTHORIZED_HEADER).sendFuture()) {
          result =>
            result.statusCode() shouldBe 200
            result.bodyAsJsonObject().get.getString("id") shouldBe dataIdentifier
        }
    }
  }

  "Malformed state update " should " be rejected" in {
    val malformedBody = Useful.postState.copy()
    malformedBody.getJsonArray("data").getJsonObject(0).remove("timestamp")
    whenReady(client.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(malformedBody)) {
      result => result.statusCode() shouldBe 400
    }
  }

  override def beforeAll(): Unit = {
    Await.result(RestBootstrap.boot(), Duration(5000, TimeUnit.MILLISECONDS))
    client = RestBootstrap.webClient()
  }

  override def afterAll(): Unit = {
    client.close()
  }
}

object Useful {
  val postState = new JsonObject("""
      {
        "data": [
            {
                "id":"",
                "value": 75.0,
                "category": "heartbeat",
                "timestamp": 134034600,
                "feeder": {
                    "name": "mi_band_3"
                }
            }
        ]
      }
      """)
  val postStateInvalidCategory = new JsonObject("""
      {
        "data": [
            {
                "value": 75.0,
                "category": "unsupported",
                "timestamp": 134034600,
                "feeder": {
                    "name": "mi_band_3"
                }
            }
        ]
      }
      """)
  val postDataGroupCategory = new JsonObject("""
      {
        "data": [
            {
                "id":"",
                "value": 75.0,
                "category": "medial",
                "timestamp": 134034600,
                "feeder": {
                    "name": "mi_band_3"
                }
            }
        ]
      }
      """)
}