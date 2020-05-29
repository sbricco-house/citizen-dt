package it.unibo.service.citizen

import it.unibo.core.data.DataCategory
import it.unibo.service.authentication.TokenIdentifier
import org.eclipse.californium.core.{CoapClient, CoapResponse, CoapServer}
import it.unibo.core.microservice.coap._

import scala.concurrent.Promise

object CoapScope {
  val CITIZEN_TOKEN = TokenIdentifier("jwt1")

  var server : CoapServer = _
  def boot() : Unit = {
    server = CoapObservableApi(CitizenMicroservices.refresh(), CitizenMicroservices.parserRegistry)
    server.start()
  }

  def teardown() : Unit = {
    server.destroy()
  }


  def isEmpty(response : CoapResponse) : Boolean = response.getResponseText == "{}"
  def createClientByCategory(category : DataCategory) : CoapClient = {
    new CoapClient(s"coap://localhost/citizen/50/state?data_category=${category.name}")
  }

  def installExpectedOne(coapClient: CoapClient) : Promise[String] = {
    val promise = Promise[String]
    val handler : CoapResponse => Unit = data => {
      if(!isEmpty(data)) {
        promise.success(data.getResponseText)
      }
    }
    coapClient.observeWithTokenAndWait(CITIZEN_TOKEN.token, handler)
    promise
  }
  def installExpectedMany(coapClient: CoapClient, howMany : Int) : Promise[Set[String]] = {
    val promise = Promise[Set[String]]
    var elements : Set[String] = Set.empty
    val handler : CoapResponse => Unit = data => {
      if(!isEmpty(data)) {
        elements += data.getResponseText
      }
      if(elements.size == howMany && !promise.isCompleted) {
        promise.success(elements)
      }
    }
    coapClient.observeWithTokenAndWait(CITIZEN_TOKEN.token, handler)
    promise
  }
}
