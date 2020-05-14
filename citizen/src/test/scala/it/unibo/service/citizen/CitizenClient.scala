package it.unibo.service.citizen
import io.vertx.scala.core.Vertx
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.dt.History.History
import it.unibo.service.authentication.SystemUser
import CitizenClient._
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.service.authentication.AuthService.MockUser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CitizenClient {
  private val STATE_PATH = s"/citizens/%s/state"
}

// TODO: finish testing
/*class CitizenClient(private val baseUri: String) extends CitizenService {

  private val vertx = Vertx.vertx()
  private val httpClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  private val stateUri = s"$baseUri$STATE_PATH"

  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): Future[Seq[Data]] = ???

  override def readState(who: SystemUser, citizenId: String): Future[Seq[Data]] = {
    httpClient.get(stateUri.format(citizenId))
      .putHeader("Authorization", citizenId)
      .sendFuture().map(r => (r.statusCode(), r.body())).onComplete {
      case Failure(exception) => println(exception)
      case Success((code, body)) => println(code, body)
    }
    Future.successful(Seq())
  }

  override def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int): Future[History] = ???

  override def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): Future[Option[Data]] = ???
}

object A extends App {
  val client = new CitizenClient("http://localhost:8080")
  client.readState(MockUser, "50").onComplete {
    case Failure(exception) => println(exception)
    case Success(value) => println(value)
  }(ExecutionContext.global)
}*/