package it.unibo.service.citizen
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.dt.History.History
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.core.client.{RestApiClient, RestClientServiceResponse}
import it.unibo.core.utils.HttpCode.Ok
import it.unibo.core.microservice._
class HttpCitizenClient(host : String, port : Int, user : String) extends CitizenService with RestApiClient with RestClientServiceResponse {
  import io.vertx.scala.core.Vertx._
  override val webClient : WebClient = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def updateState(who: TokenIdentifier, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]] = {
    parseServiceResponseWhenComplete(webClient.patch(host).sendFuture()) {
      case (Ok,_) => Seq.empty[Data]
    }.toFutureService
  }

  override def readState(who: TokenIdentifier, citizenId: String): FutureService[Seq[Data]] = ???

  override def readHistory(who: TokenIdentifier, citizenId: String, dataCategory: DataCategory, maxSize: Int): FutureService[History] = ???

  override def readHistoryData(who: TokenIdentifier, citizenId: String, dataIdentifier: String): FutureService[Data] = ???

  override def observeState(who: TokenIdentifier, citizenId: String): FutureService[Channel] = ???
}
