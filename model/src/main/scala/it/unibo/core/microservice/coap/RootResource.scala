package it.unibo.core.microservice.coap

import java.util

import org.eclipse.californium.core.CoapResource
import org.eclipse.californium.core.server.resources.Resource

case class RootResource(baseName : String, childFactory : String => CoapResource) extends CoapResource(baseName) {
  private var resources : Map[String, Resource] = Map.empty
  override def getChild(name: String): Resource = resources.getOrElse(name, {
    val newResource = childFactory(name)
    resources += name -> newResource
    newResource
  })
  import collection.JavaConverters._
  override def getChildren: util.Collection[Resource] = resources.values.toList.asJavaCollection
}