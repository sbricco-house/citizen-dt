package it.unibo.service.citizen

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx.vertx
import io.vertx.scala.core.http.ServerWebSocket
import it.unibo.core.data.Data
import it.unibo.core.microservice.protocol.{WebsocketResponse, WebsocketUpdate}
import it.unibo.core.microservice.vertx.WebSocketApi
import it.unibo.core.microservice.{Fail, Response, ServiceResponse}
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.middleware.UserMiddleware
import it.unibo.service.citizen.websocket.{CitizenProtocol, Ok, Status}
import monix.execution.{Ack, Scheduler}

import scala.concurrent.Future
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
        self.citizenService.observeState(user, self.citizenIdentifier)
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

  private def manageChannel(webSocket: ServerWebSocket, channel : CitizenService#Channel) : Unit = {
    val toCancel = channel.updateDataStream()
      .map(parser.encode)
      .collect { case Some(obj) => obj }
      .foreach(consumeData(webSocket, _))

    channel.updateDataStream()
    webSocket.closeHandler(_ => {
      channel.close()
      toCancel.cancel()
    })

    webSocket.accept()
    webSocket.textMessageHandler(handler => {
      val dataEncoded = CitizenProtocol.requestParser.encode(handler)
      dataEncoded match {
        case Some(request) =>
          val decodedElements = request.value.getAsObjectSeq.map(elements => elements.map(parser.decode))
          val anyFailedDecoding = decodedElements.map(_.contains(None))
          anyFailedDecoding match {
            case Some(false) => manageRequest(request.id, channel, decodedElements.get, webSocket)
            case Some(true) =>
              val response = WebsocketResponse[Status](request.id, CitizenProtocol.unkwonDataCategoryError)
              val jsonResponse = CitizenProtocol.responseParser.decode(response)
              webSocket.writeTextMessage(jsonResponse)
            case _ => webSocket.writeTextJsonObject(CitizenProtocol.unkwon)
          }
        case None => webSocket.writeTextJsonObject(CitizenProtocol.unkwon)
      }
    })
  }
  private def consumeData(websocket: ServerWebSocket, data : JsonObject) : Unit = {
      val updatePacket = WebsocketUpdate(data)
      websocket.writeTextMessage(CitizenProtocol.updateParser.decode(updatePacket))
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
