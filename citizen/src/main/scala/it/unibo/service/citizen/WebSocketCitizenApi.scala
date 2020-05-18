package it.unibo.service.citizen

import io.vertx.scala.core.http.ServerWebSocket
import it.unibo.core.data.Data
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.core.microservice.vertx.WebSocketApi
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.citizen.middleware.UserMiddleware

import scala.util.{Failure, Success, Try}

trait WebSocketCitizenApi extends WebSocketApi {
  self : RestCitizenVerticle =>

  override def webSocketHandler(websocket: ServerWebSocket): Unit = {
    val userOption = websocket.headers().get(UserMiddleware.AUTHORIZATION_HEADER)
    val pathCorrectness = evalPath(websocket)
    (userOption, pathCorrectness) match {
      case (None, _) => websocket.reject(401)
      case (_, Failure(_)) => websocket.reject(400)
      case (Some(user), Success(_)) =>
        self.citizenService.observeState(user, self.citizenIdentifier, createWebsocketCallback(websocket))
          .whenComplete {
            case Response(channel) => manageChannel(websocket, channel)
            case Fail(Unauthorized(m)) => websocket.reject(401)
            case _ => websocket.reject(400)
          }
    }
    websocket.path() match {
      case self.citizenStateEndpoint =>
        userOption
          .map(user => citizenService.observeState(user, citizenIdentifier, createWebsocketCallback(websocket)))

      case _ => websocket.reject(400) //TODO put right cose
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

  }
}
