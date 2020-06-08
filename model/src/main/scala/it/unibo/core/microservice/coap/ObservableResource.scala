package it.unibo.core.microservice.coap

import org.eclipse.californium.core.CoapResource
import org.eclipse.californium.core.coap.CoAP.Type

class ObservableResource(name : String, observingType : Type) extends CoapResource(name : String) {
  this.setObservable(true)
  this.setObserveType(observingType)
  getAttributes.setObservable(); // mark observable in the Link-Format
}
