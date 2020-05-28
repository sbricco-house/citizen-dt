package it.unibo.service.citizen

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.microservice.Response
import it.unibo.core.microservice.coap._
import it.unibo.core.parser.DataParserRegistry
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.CitizenMessageDelivery.ObserveData
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.eclipse.californium.core.CoapServer
import org.eclipse.californium.core.coap.CoAP.{ResponseCode, Type}
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.network.Exchange
import org.eclipse.californium.core.server.resources.{CoapExchange, Resource}

object CoapObservableApi {
  def apply(citizenService : CitizenService,
            dataParser : DataParserRegistry[JsonObject],
            port : Int = 5683) : CoapServer = {
    val server = CaopApi(port)
    val observableFactory : ObserveData => Option[Resource] = data => dataParser
      .decodeCategory(data.category)
      .map(new CategoryLink(data.id, _, citizenService, dataParser))
    val messageDelivery = new CitizenMessageDelivery(server.getRoot, observableFactory)
    server.setMessageDeliverer(messageDelivery)
    server
  }

  class CategoryLink(citizenId : String,
                     category : DataCategory,
                     citizenService : CitizenService,
                     parser : DataParserRegistry[JsonObject]) extends ObservableResource(citizenId + "/" + category, Type.NON) {
    var elements = "{}"
    var link : Option[Observable[Data]] = None
    override def handleGET(exchange: CoapExchange): Unit = {
      exchange.respond(ResponseCode.CONTENT, elements, MediaTypeRegistry.APPLICATION_JSON)
    }

    override def handleRequest(exchange: Exchange): Unit = {
      val coapExchange = new CoapExchange(exchange, this)
      val tokenOpt = coapExchange.getAuthToken
      def manageObserve(token : String): Unit = {
        val observable = citizenService.observeState(TokenIdentifier(token), category)
        observable.whenComplete {
          case Response(observable) =>
            createLink(observable, category)
            super.handleRequest(exchange)
          case _ => coapExchange.reject()
        }
      }
      def isObservableNecessary = exchange.getRequest.isObserve && ! exchange.getRelation.isEstablished
      tokenOpt match {
        case None => coapExchange.reject()
        case Some(token) if(isObservableNecessary) => manageObserve(token)
        case _ => super.handleRequest(exchange)
      }
    }


    private def createLink(observable : Observable[Data], category : DataCategory) : Unit = {
      link match {
        case None =>
          observable.foreach(data => parser.encode(data) match {
            case Some(json) =>
              elements = json.encode()
              this.changed()
            case _ =>
          })
          link = Some(observable)
        case _ =>
      }
    }
  }
}