package it.unibo.service.citizen

import java.net.Socket

import it.unibo.core.data.DataCategory
import it.unibo.service.authentication.TokenIdentifier
import org.eclipse.californium.core.{CoapClient, CoapObserveRelation, CoapResponse, CoapServer}
import it.unibo.core.microservice.coap._
import it.unibo.service.citizen.coap.CoapObservableApi
import org.eclipse.californium.core.network.{EndpointManager, RemoteEndpointManager}

import scala.concurrent.Promise

object CoapScope {
  val CITIZEN_TOKEN = TokenIdentifier("jwt1")
  var currentPort = 5683

  var server : CoapServer = _
  def boot() : Unit = {
    server = CoapObservableApi(CitizenMicroservices.citizenService, CitizenMicroservices.parserRegistry, currentPort)
    server.start()
  }

  def teardown() : Unit = {
    currentPort += 1
    EndpointManager.reset()
    server.destroy()
  }

  def isEmpty(response : CoapResponse) : Boolean = response.getResponseText == "{}" || response.getResponseText == ""
  def createClientByCategory(category : DataCategory) : CoapClient = {
    new CoapClient(s"coap://localhost:$currentPort/citizen/50/state?data_category=${category.name}")
  }

  def installExpectedOne(coapClient: CoapClient) : (Promise[String], CoapObserveRelation) = {
    val promise = Promise[String]
    val handler : CoapResponse => Unit = data => {
      if(!isEmpty(data) && !promise.isCompleted) {
        promise.success(data.getResponseText)
      }
    }
    val relation = coapClient.observeWithTokenAndWait(CITIZEN_TOKEN.token, handler)
    (promise, relation)
  }
  def installExpectedMany(coapClient: CoapClient, howMany : Int) : (Promise[Set[String]], CoapObserveRelation) = {
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
    val relation = coapClient.observeWithTokenAndWait(CITIZEN_TOKEN.token, handler)
    (promise, relation)
  }
}
