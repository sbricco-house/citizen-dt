package it.unibo.service.citizen.websocket

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{JsonArray, JsonObject}
import io.vertx.scala.core.http.ServerWebSocket
import it.unibo.core.data.Data
import it.unibo.core.microservice.protocol.{WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.core.microservice.vertx.WebSocketApi
import it.unibo.core.microservice.{Fail, Response, ServiceResponse}
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.middleware.UserMiddleware
import it.unibo.service.citizen.{CitizenService, RestCitizenVerticle}
import monix.execution.Cancelable

import scala.util.{Failure, Success, Try}

trait WebSocketCitizenApi extends WebSocketApi {
  self : RestCitizenVerticle =>
  import it.unibo.core.microservice.vertx._
  implicit lazy val monixVertxContext = monix.execution.Scheduler(VertxExecutionContext(self.vertx.getOrCreateContext()))

  override def webSocketHandler(websocket: ServerWebSocket): Unit = {
    val tokenOption = websocket.headers()
      .get(UserMiddleware.AUTHORIZATION_HEADER)
      .map(TokenIdentifier)
    val pathCorrectness = evalPath(websocket)
    (tokenOption, pathCorrectness) match {
      case (None, _) => websocket.rejectNotAuthorized()
      case (_, Failure(_)) => websocket.rejectBadContent()
      case (Some(user), Success(_)) =>
        self.citizenService.createPhysicalLink(user)
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

  private def manageChannel(webSocket: ServerWebSocket, channel : CitizenService#PhysicalLink) : Unit = {
    val clientChannel = maintainChannelFromClient(webSocket, channel)
    val serverChannel = maintainChannelFromServer(webSocket, channel)
    webSocket.closeHandler(_ => {
      channel.close()
      clientChannel.cancel()
      serverChannel.cancel()
    })
    webSocket.accept()
  }

  private def maintainChannelFromServer(webSocket: ServerWebSocket, channel : CitizenService#PhysicalLink) : Cancelable = {
    val toCancel = channel.updateDataStream()
      .map(parser.encode)
      .collect { case Some(obj) => obj }
      .foreach(consumeData(webSocket, _))

    channel.updateDataStream()
    toCancel
  }

  private def consumeData(websocket: ServerWebSocket, data : JsonObject) : Unit = {
    val updatePacket = WebsocketUpdate(data)
    websocket.writeTextMessage(CitizenProtocol.updateParser.decode(updatePacket))
  }

  private def maintainChannelFromClient(webSocket: ServerWebSocket, channel : CitizenService#PhysicalLink) : Cancelable = {
    import it.unibo.core.observable._
    val websocketObservable = observableFromWebSocket(webSocket)

    def badCategoryResponse(request : WebsocketRequest[JsonArray]) : Unit = {
      val response = WebsocketResponse[Status](request.id, CitizenProtocol.unkwonDataCategoryError)
      val jsonResponse = CitizenProtocol.responseParser.decode(response)
      webSocket.writeTextMessage(jsonResponse)
    }

    val toCancel = websocketObservable.map(CitizenProtocol.requestParser.encode)
      .fallbackOption { webSocket.writeTextJsonObject(CitizenProtocol.unkwon) }
      .collect { case Some(data) => data }
      .map(request => (request, request.value.getAsObjectSeq.map(_.map(parser.decode))))
      .fallback(_._2.nonEmpty, _ => webSocket.writeTextJsonObject(CitizenProtocol.unkwon))
      .collect { case (request, Some(data)) => (request, data)}
      .fallback(_._2.forall(_.nonEmpty), elem => badCategoryResponse(elem._1))
      .foreach {
        case (request, elem) => manageRequest(request.id, channel, elem, webSocket)
      }
    toCancel
  }

  private def manageRequest(requestId : Int, channel : CitizenService#PhysicalLink, data : Seq[Option[Data]], webSocket: ServerWebSocket) : Unit = {
    def produceResponse(futureResult : ServiceResponse[Seq[String]]) = futureResult match {
      case Response(_) =>WebsocketResponse[Status](requestId, Ok)
      case Fail(Unauthorized(_)) => WebsocketResponse[Status](requestId, CitizenProtocol.unothorized)
      case _ => WebsocketResponse[Status](requestId, CitizenProtocol.internalError)
    }
    channel.updateState(data.map(_.get)).whenComplete {
      element =>
        val result = produceResponse(element)
        webSocket.writeTextMessage(CitizenProtocol.responseParser.decode(result))
    }
  }
}
