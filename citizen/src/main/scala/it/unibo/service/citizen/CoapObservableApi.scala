package it.unibo.service.citizen

import java.util.concurrent.ScheduledThreadPoolExecutor

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.microservice.coap._
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.CitizenMessageDelivery.ObserveData
import monix.execution.CancelableFuture
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.eclipse.californium.core.coap.CoAP.{ResponseCode, Type}
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapClient, CoapResource, CoapServer}

object CoapObservableApi {
  //TODO find a better way to put this executor
  val THREAD_POOL_EXECUTOR : ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(8)

  def apply(citizenService : CitizenService,
            dataParser : DataParserRegistry[JsonObject],
            port : Int = 5683) : CoapServer = {
    val server = CaopApi(port)

    val observableFactory : ObserveData => Option[CoapResource] = data => {
      dataParser.decodeCategory(data.category)
        .map(new CategoryLink(data.id, _, citizenService, port, dataParser))
    }
    val messageDelivery = new CitizenMessageDelivery(citizenService.citizenIdentifier, observableFactory)
    server.setMessageDeliverer(messageDelivery)
    server.addDestroyListener(messageDelivery)
    server
  }

  class CategoryLink(citizenId : String,
                     category : DataCategory,
                     citizenService : CitizenService,
                     port : Int,
                     parser : DataParserRegistry[JsonObject]) extends ObservableResource(citizenId + "/" + category.name, Type.CON) {
    val coapSecret = generateCoapSecret()

    val innerClient = new CoapClient(s"coap://localhost:$port/citizen/${citizenId}/state?data_category=${category.name}")
    innerClient.setExecutors(THREAD_POOL_EXECUTOR, THREAD_POOL_EXECUTOR, false)
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
          case Response(observable) => createLink(observable, category)
          case Fail(Unauthorized(m)) => coapExchange.respond(ResponseCode.FORBIDDEN)
          case _ => coapExchange.respond(ResponseCode.INTERNAL_SERVER_ERROR)
        }
      }

      def isObservableNecessary = exchange.getRequest.isObserve && ! exchange.getRelation.isEstablished

      tokenOpt match {
        case None => coapExchange.respond(ResponseCode.UNAUTHORIZED)
        case Some(token) if(isObservableNecessary) => manageObserve(token)
        case _ =>
      }

      coapExchange.respond(ResponseCode.CONTENT, elements, MediaTypeRegistry.APPLICATION_JSON)
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
          sourceSubscription = Some(observable.foreach(subscriptionStrategy))
          categorySource = Some(observable)
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