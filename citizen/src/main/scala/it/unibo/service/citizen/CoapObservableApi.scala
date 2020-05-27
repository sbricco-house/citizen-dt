package it.unibo.service.citizen

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.Data
import it.unibo.core.microservice.Response
import it.unibo.core.microservice.coap.{RootResource, _}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.service.authentication.TokenIdentifier
import monix.reactive.Observable
import org.eclipse.californium.core.CoapResource
import org.eclipse.californium.core.coap.CoAP.Type
import org.eclipse.californium.core.network.Exchange
import org.eclipse.californium.core.server.resources.CoapExchange

object CoapObservableApi {
  def apply(citizenService : CitizenService,
            dataParser : DataParserRegistry[JsonObject]) : CoapResource = {
    RootResource("citizen", name => new Citizen(name, citizenService, dataParser))
  }

  private class Citizen(id : String, service : CitizenService, parser : DataParserRegistry[JsonObject]) extends CoapResource(id) {
    implicit val context = scala.concurrent.ExecutionContext.global
    var categoryResourceMap : Map[String, CoapResource] = Map.empty

    override def handleRequest(exchange: Exchange): Unit = {
      val coapExchange = new CoapExchange(exchange, this)
      if(exchange.getRequest.isObserve) {
        val tokenOpt = coapExchange.getAuthToken
        val categoryOpt = coapExchange.getQueryParams("data_category")
        val observableCategory = tokenOpt.zip(categoryOpt)
          .map { case (token, category) => (TokenIdentifier(token), parser.decodeCategory(category))}
          .flatMap { case (token, categoryDecoded) => categoryDecoded.map(service.observeState(token, _))}
          .headOption

        observableCategory match {
          case None => coapExchange.reject()
          case Some(service) => service.whenComplete {
            case Response(observable) => createLink(observable, categoryOpt.get, exchange)
            case _ => coapExchange.reject()
          }
        }
      }
      super.handleRequest(exchange)
    }

    private def createLink(observable : Observable[Data], category : String, exchange : Exchange) : Unit = {
      val resource = categoryResourceMap.getOrElse(category, {
        val resource = new CategoryLink(observable)
        this.add(resource)
        categoryResourceMap += category -> resource
        resource
      })
      resource.handleRequest(exchange)
    }

    //TODO trova un metodo migliore
    private class CategoryLink(data : Observable[Data]) extends ObservableResource("_", Type.NON) {
      this.setVisible(false)
      var elements = "{}"

      override def handleGET(exchange: CoapExchange): Unit = exchange.respond(elements)

      data.foreachL(data => {
        parser.encode(data)
          .map(json => json.encode())
          .foreach (data => {
            elements = data
            changed()
          })
      })
    }
  }
}