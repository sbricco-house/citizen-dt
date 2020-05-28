package it.unibo.core.microservice

import org.eclipse.californium.core.coap.Request
import org.eclipse.californium.core.{CoapClient, CoapHandler, CoapObserveRelation, CoapResponse}
import org.eclipse.californium.core.coap.{Option => CoapOption}
import org.eclipse.californium.core.server.resources.CoapExchange

package object coap {
  val TOKEN_HEADER_CODE = 2048

  implicit class RichExchangeCaop(ex : CoapExchange) {
    def getQueryParams(query : String) : Option[String] = Option(ex.getQueryParameter(query))

    import collection.JavaConverters._
    def getAuthToken : Option[String] = {
      ex.getRequestOptions.getOthers.asScala
        .filter(_.getNumber == TOKEN_HEADER_CODE)
        .map(_.getStringValue)
        .headOption
    }
  }
  implicit def funToHandler(fun : CoapResponse => Unit) : CoapHandler = new CoapHandler {
    override def onLoad(response: CoapResponse): Unit = fun(response)

    override def onError(): Unit = {}
  }
  implicit class RichClient(ex : CoapClient) {
    def observeWithToken(token : String, coapHandler: CoapHandler) : CoapObserveRelation = {
      val request = Request.newGet().setObserve()
      val options = request.getOptions
      options.addOption(new CoapOption(TOKEN_HEADER_CODE, token))
      ex.observe(request, coapHandler)
    }
    def observeWithTokenAndWait(token : String, coapHandler: CoapHandler) : CoapObserveRelation = {
      val request = Request.newGet().setObserve()
      val options = request.getOptions
      options.addOption(new CoapOption(TOKEN_HEADER_CODE, token))
      ex.observeAndWait(request, coapHandler)
    }
  }
}
