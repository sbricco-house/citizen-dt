package it.unibo.service.citizen

import java.util.UUID

import io.vertx.core.http.HttpMethod
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{HttpClient, RequestOptions}
import it.unibo.core.data.{Data, Feeder, InMemoryStorage, LeafCategory}
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.service.citizen.authorization.MockAuthorization
import it.unibo.service.citizen.controller.DefaultCitizenController
import org.scalatest._

class CitizenRestTest extends FlatSpec with Matchers with BeforeAndAfter {

  "A patch request" should "update the state" in {

  }

  val dataPatch = """
      {
        "feeder" : {
          "name" : "miband4"
        },
        "timestamp" : 102,
        "category" : "heartbeat",
        "value" : %d
      }
      """

  def generateRequestUriState : String = s"http://localhost:8080/citizen/$citizenIdVerticle/state"

  val citizenIdVerticle = "50"
  var vertx: Vertx = _
  var parserRegistry: DataParserRegistry[JsonObject] = _
  var httpClient: HttpClient = _

  before {
    vertx = Vertx.vertx()
    httpClient = vertx.createHttpClient()
    parserRegistry = DataParserRegistry(new HeartBeat.HearBeatParser())
    val store = InMemoryStorage[Data, String]()
    val authorizationFacade = MockAuthorization(HeartBeat.category)
    val controller = new DefaultCitizenController(authorizationFacade, store, parserRegistry)
    val citizenVerticle = new CitizenVerticle(controller, citizenIdVerticle)
    vertx.deployVerticle(citizenVerticle)
  }

  after {
    vertx.close()
  }

}

object HeartBeat {

  val category: LeafCategory = LeafCategory("heartbeat", 100)

  case class HeartBeatData(id: UUID, timestamp: Long, value: Double, feeder: Feeder) extends Data {
    override def category: LeafCategory = HeartBeat.category
  }

  class HearBeatParser extends VertxJsonParser {
    import it.unibo.core.microservice.vertx._
    override protected def createDataFrom(id:String, feeder: Feeder, timestamp: Long, json: JsonObject): Option[Data] = {
      json.getAsInt("value").map(v => HeartBeatData(UUID.fromString(id), timestamp, v, feeder))
    }
    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x: Double =>Some(Json.emptyObj().put("value", x))
    }
    override def target: LeafCategory = category
  }
}
