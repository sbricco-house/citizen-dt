package it.unibo.service.citizen
import io.vertx.scala.core.http.HttpClient
import it.unibo.service.citizen.HttpBootstrap.{STATE_ENDPOINT, _}
import it.unibo.service.citizen.matcher.DataJsonMatcher
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}

import scala.util.{Failure, Success}

class CitizenWebsocketApiTest extends AsyncFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures with DataJsonMatcher {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))
  var client : HttpClient = _
  "citizen api" should "can't upgrade to websocket if authorization header missing" in {
    client.webSocketFuture(STATE_ENDPOINT).transformWith {
      case Success(socket) => fail()
      case Failure(exception) => succeed
    }
  }

  "citizen api" should "enable to upgrade to websocket" in {
    client.webSocketFuture(wsOptions(STATE_ENDPOINT).putHeader(CITIZEN_AUTHORIZED_HEADER)).transformWith {
      case Success(_) => succeed
      case _ => fail()
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
