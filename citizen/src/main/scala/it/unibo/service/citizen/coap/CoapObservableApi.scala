package it.unibo.service.citizen.coap

import java.util.concurrent.ScheduledThreadPoolExecutor

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.microservice.coap.{CaopApi, JsonFormat, ObservableResource, generateCoapSecret}
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.CitizenDigitalTwin
import it.unibo.service.citizen.coap.CitizenMessageDelivery.ObserveData
import it.unibo.core.microservice.coap._
import monix.execution.CancelableFuture
import monix.reactive.Observable
import org.eclipse.californium.core.coap.CoAP.{ResponseCode, Type}
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapClient, CoapResource, CoapServer}
//implicits
import monix.execution.Scheduler.Implicits.global

object CoapObservableApi {
  private val THREAD_POOL_EXECUTOR : ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(8)

  /**
   * create a coap server that manage observe state request to a specific citizen
   * @param citizenService: the citizen service that mantain a citizen digital twin
   * @param dataParser
   * @param port
   * @param clientExecutor
   * @return
   */
  def apply(citizenService : CitizenDigitalTwin,
            dataParser : DataParserRegistry[JsonObject],
            port : Int = 5683,
            clientExecutor: ScheduledThreadPoolExecutor = THREAD_POOL_EXECUTOR) : CoapServer = {
    val server = CaopApi(port)

    val observableFactory : ObserveData => Option[CoapResource] = data => {
      dataParser.decodeCategory(data.category)
        .map(new CategoryDataSource(data.id, _, citizenService, port, dataParser, clientExecutor))
    }
    val messageDelivery = new CitizenMessageDelivery(citizenService.citizenIdentifier, observableFactory)
    server.setMessageDeliverer(messageDelivery)
    server.addDestroyListener(messageDelivery)
    server
  }

  private class CategoryDataSource(citizenId : String,
                                   category : DataCategory,
                                   citizenService : CitizenDigitalTwin,
                                   port : Int,
                                   parser : DataParserRegistry[JsonObject],
                                   scheduledThreadPoolExecutor: ScheduledThreadPoolExecutor) extends ObservableResource(citizenId + "/" + category.name, Type.CON) {
    val coapSecret = generateCoapSecret()

    val innerClient = new CoapClient(s"coap://localhost:$port/citizen/${citizenId}/state?data_category=${category.name}")
    innerClient.setExecutors(scheduledThreadPoolExecutor, scheduledThreadPoolExecutor, false)
    var elements = "{}"
    var categorySource : Option[Observable[Data]] = None
    var sourceSubscription : Option[CancelableFuture[_]] = None

    override def delete(): Unit = {
      super.delete()
      sourceSubscription.foreach(_.cancel())
      innerClient.shutdown()
    }
    override def handleGET(coapExchange: CoapExchange): Unit = {
      val exchange = coapExchange.advanced()
      val tokenOpt = coapExchange.getAuthToken

      def manageObserve(token : String): Unit = {
        val observable = citizenService.observeState(TokenIdentifier(token), category)
        observable.whenComplete {
          case Response(observable) =>
            createLink(observable, category)
            coapExchange.respond(ResponseCode.CONTENT, elements, MediaTypeRegistry.APPLICATION_JSON)
          case Fail(Unauthorized(m)) => coapExchange.respond(ResponseCode.FORBIDDEN)
          case _ => coapExchange.respond(ResponseCode.INTERNAL_SERVER_ERROR)
        }
      }

      def isObservableNecessary = exchange.getRequest.isObserve && ! exchange.getRelation.isEstablished

      tokenOpt match {
        case None => coapExchange.respond(ResponseCode.UNAUTHORIZED)
        case Some(token) if(isObservableNecessary) => manageObserve(token)
        case Some(token) => coapExchange.respond(ResponseCode.CONTENT, elements, MediaTypeRegistry.APPLICATION_JSON)
      }
    }

    private def createLink(observable : Observable[Data], category : DataCategory) : Unit = {
      def subscriptionStrategy(data : Data) = parser.encode(data) match {
        case Some(json) =>
          elements = json.encode()
          innerClient.putWithOptions(elements, JsonFormat, coapSecret)
        case _ =>
      }
      categorySource match {
        case None =>
          categorySource = Some(observable)
          sourceSubscription = Some(observable.foreach(subscriptionStrategy))
        case _ =>
      }
    }

    override def handlePUT(exchange: CoapExchange): Unit = {
      exchange.getOption(coapSecret._1) match {
        case Some(this.coapSecret._2) =>
          elements = exchange.getRequestText
          changed()
          exchange.respond(ResponseCode.CHANGED)
        case _ => super.handlePUT(exchange)
      }
    }
  }
}
