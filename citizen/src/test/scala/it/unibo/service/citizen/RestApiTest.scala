package it.unibo.service.citizen

import io.vertx.core.json.JsonObject
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.service.citizen.RestBootstrap._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncFlatSpec, BeforeAndAfterAll, Matchers}

class RestApiTest extends AsyncFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures {

  private var client: WebClient = _

  "Unauthenticated user" should " should not see a citizen state" in {
    whenReady(client.get(STATE_ENDPOINT).sendFuture()) {
      result => result.statusCode() shouldBe 401
    }
  }

  "Authorized user " should " update the citizen state" in {
    whenReady(client.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(Useful.postState)) {
      result => result.statusCode() shouldBe 204
    }
  }

  // TODO: need to pass
  "User cannot " should " write data of unsupported category" in {
    whenReady(client.patch(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendJsonObjectFuture(Useful.postStateInvalidCategory)) {
      result => result.statusCode() shouldBe 400
    }
  }

  "Citizen " should " see his state" in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER).sendFuture()) {
      result => result.statusCode() shouldBe 200; result.bodyAsJsonObject().get shouldBe Useful.postState
    }
  }

  "Unauthorized Stakeholder" should " not see a citizen state" in {
    whenReady(client.get(STATE_ENDPOINT).putHeader(STAKEHOLDER_AUTHORIZED_HEADER).sendFuture()) {
      result => println(result.body()); result.statusCode() shouldBe 403
    }
  }

  "History " should " read from citizen " in {
    whenReady(client.get(HISTORY_ENDPOINT)
      .putHeader(CITIZEN_AUTHORIZED_HEADER)
      .addQueryParam("data_category", HISTORY_CATEGORY)
      .addQueryParam("limit", HISTORY_LIMIT.toString).sendFuture())
    {
      result => result.statusCode() shouldBe 200; result.bodyAsJsonArray().get shouldBe Useful.postState.getJsonArray("data")
    }
  }


  override def beforeAll(): Unit = {
    client = RestBootstrap.boot()
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
}