package it.unibo.service.citizen

import io.vertx.scala.core.http.ServerWebSocket
import it.unibo.core.data.Data
import it.unibo.core.microservice.protocol.WebsocketResponse
import it.unibo.core.microservice.vertx.WebSocketApi
import it.unibo.core.microservice.{Fail, Response, ServiceResponse}
import it.unibo.core.protocol.ServiceError.Unauthorized
import it.unibo.service.authentication.JWToken
import it.unibo.service.citizen.middleware.UserMiddleware
import it.unibo.service.citizen.websocket.{CitizenProtocol, Ok, Status}

import scala.util.{Failure, Success, Try}

trait WebSocketCitizenApi extends WebSocketApi {
  self : RestCitizenVerticle =>
  import it.unibo.core.microservice.vertx._

  override def webSocketHandler(websocket: ServerWebSocket): Unit = {
    val tokenOption = websocket.headers().get(UserMiddleware.AUTHORIZATION_HEADER)
      .map(JWToken)
    val pathCorrectness = evalPath(websocket)
    (tokenOption, pathCorrectness) match {
      case (None, _) => websocket.rejectNotAuthorized()
      case (_, Failure(_)) => websocket.rejectBadContent()
      case (Some(user), Success(_)) =>
        self.citizenService.observeState(user, self.citizenIdentifier, createWebsocketCallback(websocket))
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

  private def createWebsocketCallback(websocket: ServerWebSocket) : Data => Unit = data => parser.encode(data) match {
    case Some(encoded) => websocket.writeBinaryMessage(encoded.toBuffer)
    case _ => //TODO close the socket? or someone?
  }

  private def manageChannel(webSocket: ServerWebSocket, channel : CitizenService#Channel) : Unit = {
    webSocket.closeHandler(_ => channel.close())

    webSocket.textMessageHandler(handler => {
      val dataEncoded = CitizenProtocol.requestParser.encode(handler)
      dataEncoded match {
        case Some(request) =>
          val decodedElements = request.value.getAsObjectSeq.map(elements => elements.map(parser.decode))
          val anyFailedDecoding = decodedElements.map(_.contains(None))
          anyFailedDecoding match {
            case Some(true) => manageRequest(request.id, channel, decodedElements.get, webSocket)
            case Some(false) =>
              val response = WebsocketResponse[Status](request.id, CitizenProtocol.unkwonDataError)
              val jsonResponse = CitizenProtocol.responseParser.decode(response)
              webSocket.writeTextMessage(jsonResponse)
            case _ => webSocket.writeTextJsonObject(CitizenProtocol.unkwon)
          }
        case None => webSocket.writeTextJsonObject(CitizenProtocol.unkwon)
      }
    })
  }

  private def manageRequest(requestId : Int, channel : CitizenService#Channel, data : Seq[Option[Data]], webSocket: ServerWebSocket) : Unit = {
    def produceResponse(futureResult : ServiceResponse[Seq[Data]]) = futureResult match {
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
