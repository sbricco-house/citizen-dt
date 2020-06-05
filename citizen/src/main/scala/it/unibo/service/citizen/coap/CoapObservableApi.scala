package it.unibo.service.citizen.coap

import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledThreadPoolExecutor}

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.microservice.coap.{CaopApi, JsonFormat, ObservableResource, generateCoapSecret}
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.citizen.CitizenDigitalTwin
import it.unibo.service.citizen.coap.CitizenMessageDelivery.ObserveData
import it.unibo.core.microservice.coap._
import monix.eval.Task
import monix.execution.{Ack, CancelableFuture, ExecutionModel, Scheduler}
import monix.execution.ExecutionModel.AlwaysAsyncExecution
import monix.reactive.{Observable, OverflowStrategy}
import monix.reactive.observers.Subscriber
import org.eclipse.californium.core.coap.CoAP.{ResponseCode, Type}
import org.eclipse.californium.core.coap.MediaTypeRegistry
import org.eclipse.californium.core.server.resources.CoapExchange
import org.eclipse.californium.core.{CoapClient, CoapResource, CoapServer}

import scala.concurrent.{ExecutionContext, Future}
//implicits
object CoapObservableApi {
  implicit val monixContext =  monix.execution.Scheduler.singleThread("coap-monix", executionModel = AlwaysAsyncExecution)

  private def createThreadPool() : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  /**
   * Create a coap server that manage observe state request to a specific citizen
   * @param citizenService The citizen service that maintain a citizen digital twin
   * @param dataParser The parser used to marshalL/unmarshall data
   * @param port In which the CoaP server is installed, Default 5683
   * @param serverExecutor the executor used by server to handle the request. It need 2 Thread at least
   * @return
   */
  def apply(citizenService : CitizenDigitalTwin,
            dataParser : DataParserRegistry[JsonObject],
            port : Int = 5683,
            serverExecutor: ScheduledExecutorService = createThreadPool()) : CoapServer = {
    val server = CaopApi(port)

    val observableFactory : ObserveData => Option[CoapResource] = data => {
      dataParser.decodeCategory(data.category)
        .map(new CategoryDataSource(data.id, _, citizenService, port, dataParser, serverExecutor))
    }
    val messageDelivery = new CitizenMessageDelivery(citizenService.citizenIdentifier, observableFactory)
    server.setMessageDeliverer(messageDelivery)
    server.addDestroyListener(messageDelivery)
    server.setExecutors(serverExecutor, serverExecutor, false)
    server
  }

  //internal implementation of coap resource that handle get and observe request.
  private class CategoryDataSource(citizenId : String,
                                   category : DataCategory,
                                   citizenService : CitizenDigitalTwin,
                                   port : Int,
                                   parser : DataParserRegistry[JsonObject],
                                   executor : ScheduledExecutorService) extends ObservableResource(citizenId + "/" + category.name, Type.CON) {
    private val coapSecret = generateCoapSecret()
    private val futureContext = ExecutionContext.fromExecutor(executor)

    val innerClient = new CoapClient(s"coap://localhost:$port/citizen/${citizenId}/state?data_category=${category.name}")
    var elements = "{}"
    var sourceSubscription : Option[CancelableFuture[_]] = None

    override def delete(): Unit = {
      super.delete()
      sourceSubscription.foreach(_.cancel())
      innerClient.shutdown()
    }
    override def handleGET(coapExchange: CoapExchange): Unit = {
      val exchange = coapExchange.advanced()
      val tokenOpt = coapExchange.getAuthToken
      def manageObserve(token : String): Unit = {
        val observable = citizenService.observeState(TokenIdentifier(token), category)
        observable.whenComplete {
          case Response(observable) =>
            createLink(observable, category)
            coapExchange.respond(ResponseCode.CONTENT, "{}", MediaTypeRegistry.APPLICATION_JSON)
          case Fail(Unauthorized(m)) => coapExchange.respond(ResponseCode.FORBIDDEN)
          case _ => coapExchange.respond(ResponseCode.INTERNAL_SERVER_ERROR)
        }(futureContext)
      }
      def isObservableNecessary = exchange.getRequest.isObserve && ! exchange.getRelation.isEstablished
      def isAlreadyObserved = exchange.getRelation != null && exchange.getRelation.isEstablished
      tokenOpt match {
        case None => coapExchange.respond(ResponseCode.UNAUTHORIZED)
        //create the observer link
        case Some(token) if isObservableNecessary => manageObserve(token)
        //the client has been authenticated and the relation is established
        case Some(_) if isAlreadyObserved => coapExchange.respond(ResponseCode.CONTENT, elements, MediaTypeRegistry.APPLICATION_JSON)
        //currently, standard get is not supported.
        case Some(token) => super.handleGET(coapExchange)
      }
    }
    private def createLink(observable : Observable[Data], category : DataCategory) : Unit = {
      def subscriptionStrategy(data : Data) = parser.encode(data) match {
        case Some(json) =>
          elements = json.encode()
          innerClient.putWithOptions(elements, JsonFormat, coapSecret)
        case _ =>
      }
      sourceSubscription match {
        case None => sourceSubscription = Some(observable.observeOn(monixContext).foreach(subscriptionStrategy))
        case _ =>
      }
    }

    override def handlePUT(exchange: CoapExchange): Unit = {
      exchange.getOption(coapSecret._1) match {
        case Some(this.coapSecret._2) =>
          elements = exchange.getRequestText
          changed()
          exchange.respond(ResponseCode.CHANGED)
        case _ => super.handlePUT(exchange)
      }
    }
  }
}
