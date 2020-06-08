package it.unibo.core.microservice.coap

import it.unibo.core.microservice.coap.ScalaCoapServer.DestroyListener
import org.eclipse.californium.core.CoapServer

class ScalaCoapServer(port : Int) extends CoapServer(port) {
  var listeners : List[DestroyListener] = List.empty
  def addDestroyListener(listner : DestroyListener) : Unit = listeners ::= listner

  override def destroy(): Unit = {
    super.destroy()
    listeners.foreach{_()}
  }
  override def stop(): Unit = {
    super.stop()
    listeners.foreach{_()}
  }
}

object ScalaCoapServer {
  type DestroyListener = Unit => Unit
}

