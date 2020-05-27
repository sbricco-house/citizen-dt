package it.unibo.core.microservice

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
}
