package it.unibo.core.microservice

import java.util.UUID

import org.eclipse.californium.core.coap.{Request, Option => CoapOption}
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapClient, CoapHandler, CoapObserveRelation, CoapResponse}

import scala.util.Random

package object coap {
  private val MAX_OTHER_OPTIONS = 4080
  val TOKEN_HEADER_CODE = 2048
  val FIRST_FREE_POSITION = 2049
  val rand = new Random()

  def generateCoapSecret() : (Int, String) = {
    val key = rand.nextInt(MAX_OTHER_OPTIONS) + FIRST_FREE_POSITION
    val string = UUID.randomUUID().toString
    (key, string)
  }
  implicit class RichExchangeCaop(ex : CoapExchange) {
    def getQueryParams(query : String) : Option[String] = Option(ex.getQueryParameter(query))

    import collection.JavaConverters._
    def getAuthToken : Option[String] = getOption(TOKEN_HEADER_CODE)

    def getOption(code : Int) : Option[String] = {
      ex.getRequestOptions.getOthers.asScala
        .filter(_.getNumber == code)
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
      ex.getEndpoint
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

    def getWithToken(token : String) : CoapResponse = {
      val request = Request.newGet()
      val options = request.getOptions
      options.addOption(new CoapOption(TOKEN_HEADER_CODE, token))
      ex.advanced(request)
    }

    def putWithOptions(payload : String, contentFormat: ContentFormat, optionSequence : (Int, String) *): CoapResponse = {
      val request = Request.newPut()
      request.setPayload(payload)
      val options = request.getOptions
      options.setContentFormat(contentFormat.code)
      optionSequence.foreach {
        case (key, value) => options.addOption(new CoapOption(key, value))
      }
      ex.advanced(request)
    }
  }
}
