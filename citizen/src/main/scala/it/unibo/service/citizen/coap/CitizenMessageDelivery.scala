package it.unibo.service.citizen.coap

import io.lemonlabs.uri.Uri
import it.unibo.core.microservice.coap.ScalaCoapServer
import it.unibo.service.citizen.coap.CitizenMessageDelivery.ObserveData
import org.eclipse.californium.core.CoapResource
import org.eclipse.californium.core.coap.Request
import org.eclipse.californium.core.network.Exchange
import org.eclipse.californium.core.server.ServerMessageDeliverer
import org.eclipse.californium.core.server.resources.Resource

/**
 * a specific implementation of MessageDelivery for CoapServer. This implementation allow to
 * manage variable path (e.g. /citizen/{id}/state?data_category=heartbeat).
 * @param citizenIdentifier the citizen identifier accepted by this message router
 * @param observableResourceFactory the factory used to create the coap resource associated to a specific citizen id and a specific category
 */
class CitizenMessageDelivery(citizenIdentifier : String, observableResourceFactory : ObserveData => Option[CoapResource]) extends ServerMessageDeliverer(new CoapResource(".hide")) with ScalaCoapServer.DestroyListener {
  var resources : Map[String, CoapResource] = Map.empty[String, CoapResource]
  import collection.JavaConverters._
  /*
   * this method contains the logic for creating "observable state" resource on demand, according the
   * request.
   * an observing request on a path /citizen/{id}/state?data_category=x produce a creation (or reuse)
   * of a ObservableCoapResource (i.e. a CoapResource setted to be observed).
   */
  override def findResource (exchange: Exchange): Resource = {
    val uri = exchange.getRequest.getURI

    validObservablePath(exchange.getRequest) match {
      case Some(category) => resources.get(uri)
        .orElse({
          val newResource = observableResourceFactory(category)
          newResource.foreach(resources += uri -> _)
          newResource
        })
        .getOrElse(super.findResource(exchange))
      case _ => null
    }
  }
  /*
   * a valid path follows this pattern : /citizen/{citizenIdentifier}/state?data_category="acategory"
   */
  private def validObservablePath(request : Request) : Option[ObserveData] = {
    val elements = request.getOptions.getUriPath.asScala.toList
    val scalaUri = Uri.parse(request.getURI)
    val categoryOpt = scalaUri.toUrl.query.param("data_category")
    elements match {
      case ("citizen" :: this.citizenIdentifier :: "state" :: Nil) => categoryOpt.map(ObserveData(citizenIdentifier, _))
      case _ => None
    }
  }

  override def apply(v1: Unit) = this.resources.values.foreach(_.delete())
}

object CitizenMessageDelivery {

  /**
   * an utility class that wrap the main data used to manage an observing request.
   * @param id the citizen id that will observed
   * @param category the category selected
   */
  case class ObserveData(id : String, category : String)
}
