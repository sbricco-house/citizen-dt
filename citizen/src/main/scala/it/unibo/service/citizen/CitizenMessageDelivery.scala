package it.unibo.service.citizen

import io.lemonlabs.uri.Uri
import it.unibo.service.citizen.CitizenMessageDelivery.ObserveData
import org.eclipse.californium.core.coap.Request
import org.eclipse.californium.core.network.Exchange
import org.eclipse.californium.core.server.ServerMessageDeliverer
import org.eclipse.californium.core.server.resources.Resource

class CitizenMessageDelivery(root : Resource, observableResourceFactory : ObserveData => Option[Resource]) extends ServerMessageDeliverer(root) {
  var resources : Map[String, Resource] = Map.empty[String, Resource]
  import collection.JavaConverters._
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
      case _ => super.findResource(exchange)
    }
  }

  def validObservablePath(request : Request) : Option[ObserveData] = {
    val elements = request.getOptions.getUriPath.asScala.toList
    val scalaUri = Uri.parse(request.getURI)
    val categoryOpt = scalaUri.toUrl.query.param("data_category")
    elements match {
      case ("citizen" :: id :: state :: Nil) => categoryOpt.map(ObserveData(id, _))
      case _ => None
    }
  }
}

object CitizenMessageDelivery {
  case class ObserveData(id : String, category : String)
}
