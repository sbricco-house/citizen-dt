package it.unibo.service.citizen.websocket

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{JsonArray, JsonObject}
import io.vertx.scala.core.http.ServerWebSocket
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.authentication.middleware.UserMiddleware
import it.unibo.core.data.Data
import it.unibo.core.microservice.protocol.{WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.core.microservice.vertx.WebSocketApi
import it.unibo.core.microservice.{Fail, Response, ServiceResponse}
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.citizen.{CitizenDigitalTwin, CitizenVerticle}
import monix.execution.Cancelable

import scala.util.{Failure, Success, Try}

/**
 * Websocket decoration for Citizen Verticle.
 * an example of usage (a là Cake pattern) follows:
 *
 * new CitizenVerticle(...) with WebSocketApi
 *
 */
trait WebSocketCitizenApi extends WebSocketApi {
  self : CitizenVerticle =>
  import it.unibo.core.microservice.vertx._
  implicit lazy private val monixVertxContext = monix.execution.Scheduler(VertxExecutionContext(self.vertx.getOrCreateContext()))

  override def webSocketHandler(websocket: ServerWebSocket): Unit = {
    val tokenOption = websocket.headers()
      .get(UserMiddleware.AUTHORIZATION_HEADER)
      .flatMap(UserMiddleware.extractToken)
      .map(TokenIdentifier)
    val pathCorrectness = evalPath(websocket)
    (tokenOption, pathCorrectness) match {
      case (None, _) => websocket.rejectNotAuthorized()
      case (_, Failure(_)) => websocket.rejectBadContent()
      case (Some(user), Success(_)) =>
        self.citizenDT.createPhysicalLink(user)
          .whenComplete {
            case Response(channel) => manageChannel(websocket, channel)
            case Fail(Unauthorized(m)) => websocket.rejectNotAuthorized()
            case _ => websocket.rejectInternalError()
          }
    }
  }

  def evalPath(webSocket: ServerWebSocket) : Try[Unit] = webSocket.path() match {
    case self.citizenStateEndpoint => Success()
    case _ => Failure(new IllegalArgumentException())
  }

  private def manageChannel(webSocket: ServerWebSocket, channel : CitizenDigitalTwin#PhysicalLink) : Unit = {
    val clientChannel = maintainChannelFromClient(webSocket, channel)
    val serverChannel = maintainChannelFromServer(webSocket, channel)
    webSocket.closeHandler(_ => {
      channel.close()
      clientChannel.cancel()
      serverChannel.cancel()
    })
    webSocket.accept()
  }

  private def maintainChannelFromServer(webSocket: ServerWebSocket, channel : CitizenDigitalTwin#PhysicalLink) : Cancelable = {
    val toCancel = channel.updateDataStream()
      .map(parser.encode)
      .collect { case Some(obj) => obj }
      .foreach(consumeData(webSocket, _))

    channel.updateDataStream()
    toCancel
  }

  private def consumeData(websocket: ServerWebSocket, data : JsonObject) : Unit = {
    val updatePacket = WebsocketUpdate(data)
    websocket.writeTextMessage(CitizenProtocol.updateParser.encode(updatePacket))
  }

  private def maintainChannelFromClient(webSocket: ServerWebSocket, channel : CitizenDigitalTwin#PhysicalLink) : Cancelable = {
    import it.unibo.core.observable._
    val websocketObservable = observableFromWebSocket(webSocket)

    def badCategoryResponse(request : WebsocketRequest[JsonArray]) : Unit = {
      val response = WebsocketResponse[Status](request.id, CitizenProtocol.unknownDataCategoryError)
      val jsonResponse = CitizenProtocol.responseParser.encode(response)
      webSocket.writeTextMessage(jsonResponse)
    }
    val toCancel = websocketObservable.map(CitizenProtocol.requestParser.decode)
      .fallbackOption { webSocket.writeTextJsonObject(CitizenProtocol.unknown) }
      .collect { case Some(data) => data }
      .map(request => (request, jsonArrayToData(request.value)))
      .fallback(_._2.nonEmpty, elem => badCategoryResponse(elem._1))
      .collect { case (request, Some(data)) => (request, data)}
      .foreach {
        case (request, elem) => manageRequest(request.id, channel, elem, webSocket)
      }
    toCancel
  }

  private def manageRequest(requestId : Int, channel : CitizenDigitalTwin#PhysicalLink, data : Seq[Data], webSocket: ServerWebSocket) : Unit = {
    def produceResponse(futureResult : ServiceResponse[Seq[String]]) = futureResult match {
      case Response(_) => WebsocketResponse[Status](requestId, Ok)
      case Fail(Unauthorized(_)) => WebsocketResponse[Status](requestId, CitizenProtocol.unauthorized)
      case _ => WebsocketResponse[Status](requestId, CitizenProtocol.internalError)
    }
    channel.updateState(data).whenComplete {
      element =>
        val result = produceResponse(element)
        webSocket.writeTextMessage(CitizenProtocol.responseParser.encode(result))
    }
  }
}
