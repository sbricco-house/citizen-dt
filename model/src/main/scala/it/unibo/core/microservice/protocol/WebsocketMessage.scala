package it.unibo.core.microservice.protocol

sealed class WebsocketMessage[R]

case class WebsocketRequest[R](id : Int, value : R) extends WebsocketMessage[R]
case class WebsocketResponse[R](id : Int, value : R) extends WebsocketMessage[R]
case class WebsocketUpdate[R](value : R) extends WebsocketMessage[R]
case class WebsocketFailure(code : Int, reason : String) extends WebsocketMessage[Unit]