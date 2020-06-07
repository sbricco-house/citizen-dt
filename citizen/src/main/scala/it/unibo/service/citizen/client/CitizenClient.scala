package it.unibo.service.citizen.client

import java.net.URI
import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, Executors}

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.http.{WebSocketBase, WebSocketConnectOptions}
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.authentication.middleware.UserMiddleware
import it.unibo.core.client.{RestApiClient, RestClientServiceResponse}
import it.unibo.core.data.{Data, DataCategory}
import it.unibo.core.dt.History.History
import it.unibo.core.microservice.coap._
import it.unibo.core.microservice.protocol.{WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.core.microservice.vertx._
import it.unibo.core.microservice.{FutureService, Response, ServiceResponse}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.HttpCode
import it.unibo.service.citizen.CitizenDigitalTwin
import it.unibo.service.citizen.websocket.{CitizenProtocol, Failed, Ok}
import monix.execution.atomic.AtomicInt
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject
import org.eclipse.californium.core.{CoapClient, CoapObserveRelation, CoapResponse}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class CitizenClient(override val citizenIdentifier : String,
                    registry : DataParserRegistry[JsonObject],
                    host : String = "localhost",
                    httpPort : Int = 8080,
                    coapPort : Int = 5683) extends CitizenDigitalTwin  with RestApiClient with RestClientServiceResponse {
  private val vertx = Vertx.vertx()
  private val coapFixedThreadpool = Executors.newFixedThreadPool(4)
  implicit lazy val ctx = VertxExecutionContext(vertx.getOrCreateContext())
  private val httpClient = vertx.createHttpClient()
  private val atomicInt = AtomicInt(0)
  private def stateEndpoint(port : Int) = s"${host}:${port}/citizens/$citizenIdentifier/state"
  private val httpStateEndpoint = s"/citizens/$citizenIdentifier/state"
  private val coapStateEndpoint = s"coap://${stateEndpoint(coapPort)}"
  private val historyEndpoint = s"${host}:${httpPort}/citizens/$citizenIdentifier/history"

  override val webClient: WebClient = WebClient.create(vertx, WebClientOptions().setDefaultHost(host).setDefaultPort(httpPort))

  private def authorizationHeader(token : TokenIdentifier) : (String, String) = "Authorization" -> UserMiddleware.asToken(token.token)

  private def enrichPathWithCategory(basePath : String, data : DataCategory) : String = basePath + s"?data_category=${data.name}"

  override def updateState(who: TokenIdentifier, data: Seq[Data]): FutureService[Seq[String]] = {
    val jsonData = data.map(registry.encode).collect { case Some(data) => data }
    val jsonArray = Json.arr(jsonData:_*)
    val jsonPayload = Json.obj("data" -> jsonArray)
    val request = webClient.patch(httpStateEndpoint).putHeader(authorizationHeader(who)).sendJsonObjectFuture(jsonPayload)

    parseServiceResponseWhenComplete(request) {
      case (HttpCode.Ok, data) => Json.fromArrayString(data).getAsStringSeq.get
    }.toFutureService
  }
  override def readState(who: TokenIdentifier): FutureService[Seq[Data]] = {
    val request = webClient.get(httpStateEndpoint).putHeader(authorizationHeader(who)).sendFuture()
    parseServiceResponseWhenComplete(request) {
      case (HttpCode.Ok, data) => parseToSequence(data)
    }.toFutureService
  }
  override def readStateByCategory(who: TokenIdentifier, category: DataCategory): FutureService[Seq[Data]] = {
    val request = webClient.get(enrichPathWithCategory(httpStateEndpoint, category)).putHeader(authorizationHeader(who)).sendFuture()
    parseServiceResponseWhenComplete(request) {
      case (HttpCode.Ok, data) => parseToSequence(data)
    }.toFutureService
  }
  override def readHistory(who: TokenIdentifier, dataCategory: DataCategory, maxSize: Int): FutureService[History] = {
    val request = webClient.get(enrichPathWithCategory(historyEndpoint, dataCategory)).putHeader(authorizationHeader(who)).sendFuture()
    parseServiceResponseWhenComplete(request) {
      case (HttpCode.Ok, data) => parseToSequence(data)
    }.toFutureService
  }
  override def readHistoryData(who: TokenIdentifier, dataIdentifier: String): FutureService[Data] = {
    val request = webClient.get(historyEndpoint).putHeader(authorizationHeader(who)).sendFuture()
    parseServiceResponseWhenComplete(request) {
      case (HttpCode.Ok, data) => parseData(data).get //TODO fix
    }.toFutureService
  }

  override def createPhysicalLink(who: TokenIdentifier): FutureService[PhysicalLink] = {
    val (header, value) = authorizationHeader(who)
    val option = WebSocketConnectOptions()
      .setHost(host)
      .setPort(httpPort)
      .setURI(httpStateEndpoint)
      .addHeader(header, value)
    httpClient.webSocketFuture(option).transform {
      case Success(webSocket) => Success(Response(new InnerPhysicalLink(webSocket)))
      case Failure(reason) => Failure[ServiceResponse[PhysicalLink]](reason)
    }.toFutureService
  }

  override def observeState(who: TokenIdentifier, dataCategory: DataCategory): FutureService[Observable[Data]] = {
    implicit val coapContext = scala.concurrent.ExecutionContext.fromExecutor(coapFixedThreadpool)
    val coapClient = new CoapClient(enrichPathWithCategory(coapStateEndpoint, dataCategory))
    val source = PublishSubject[Data]()
    Future[CoapObserveRelation] {
      coapClient.observeWithTokenAndWait(who.token, (result : CoapResponse) => {
        val publishResult = JsonConversion.objectFromString(result.getResponseText) match {
          case None =>
          case Some(element) => registry.decode(element) match {
            case Some(data) => source.onNext(data)
            case _ =>
          }
        }
      })
    }(coapContext).transform {
      case Success(coapResponse) if coapResponse.isCanceled => Failure(new RuntimeException("observe failed"))
      case Success(coapResponse) => Success(Response(source))
    }(coapContext).toFutureService
  }
  //TODO manage in a better way
  private def parseToSequence(response : String) : Seq[Data] = {
    val obj = JsonConversion.objectFromString(response).getOrElse(Json.obj("data"->Json.emptyArr()))
    obj.getJsonArray("data").getAsObjectSeq match {
      case None => Seq.empty
      case Some(elements) => elements.map(registry.decode).collect { case Some(data) => data }
    }
  }

  private def parseData(response : String) : Option[Data] = {
    JsonConversion.objectFromString(response) match {
      case Some(elem) => registry.decode(elem)
      case _ => None
    }
  }

  private class InnerPhysicalLink(websocket : WebSocketBase) extends PhysicalLink {
    import CitizenProtocol._
    var promiseMap : ConcurrentMap[Int, Promise[Response[Seq[String]]]] = new ConcurrentHashMap()

    websocket.textMessageHandler(text => (updateParser.decode(text), responseParser.decode(text)) match {
      case (Some(WebsocketUpdate(json)), None) => registry.decode(json).foreach(subject.onNext)
      case (None, Some(WebsocketResponse(id, status))) =>
        val promise = promiseMap.get(id)
        status match {
          case Ok(seq) => promise.success(Response(seq)) //TODO FIX
          case Failed(reason) => promise.failure(new Exception(reason))
        }
      case _ =>
    })

    override def updateState(data: Seq[Data]): FutureService[Seq[String]] = {
      val jsonToSend = data.map(registry.encode).collect { case Some(data) => data }
      val array = Json.arr(jsonToSend:_*)
      val requestId = atomicInt.incrementAndGet()
      val request = WebsocketRequest[JsonArray](requestId, array)
      val requestJson = CitizenProtocol.requestParser.encode(request)
      val promise = Promise[Response[Seq[String]]]()
      promiseMap.put(requestId, promise)
      websocket.writeTextMessage(requestJson)
      promise.future.toFutureService
    }
    private val subject = PublishSubject[Data]()
    override val updateDataStream: Observable[Data] = subject.executeAsync

    override def close(): Unit = {
      promiseMap.clear()
      websocket.close()
    }
  }
}
