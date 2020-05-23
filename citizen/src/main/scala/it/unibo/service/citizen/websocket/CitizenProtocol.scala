package it.unibo.service.citizen.websocket

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.microservice.protocol.{WebsocketFailure, WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.core.microservice.vertx.{JsonConversion, _}
import it.unibo.core.parser.Parser

object CitizenProtocol {
  import Status._
  //TODO put in unkwon the message sent.
  val unkwon = Json.obj("reason" -> "unkwon protocol", "code" -> 500)
  val unkwonDataCategoryError = Failed("unkwon data")
  val internalError = Failed("internal error")
  val unothorized = Failed("category not allowed")

  val responseParser = Parser[String, WebsocketResponse[Status]]{
    rep => Json.obj(
      "id" -> rep.id,
      "value" -> rep.value.toJson
    ).toString
  }{
    data => for {
      json <- JsonConversion.objectFromString(data)
      id <- json.getAsInt("id")
      valueJson <- json.getAsObject("value")
      status <- Status.fromJson(valueJson)
    } yield WebsocketResponse[Status](id, status)
  }

  val requestParser  = Parser[String, WebsocketRequest[JsonArray]]{
    rep => Json.obj(
      "id" -> rep.id,
      "value" -> rep.value
    ).toString
  }{
    data => for {
      json <- JsonConversion.objectFromString(data)
      id <- json.getAsInt("id")
      value <- json.getAsArray("value")
    } yield WebsocketRequest[JsonArray](id, value)
  }

  val updateParser  = Parser[String, WebsocketUpdate[JsonObject]]{
    rep => Json.obj(
      "value" -> rep.value
    ).toString
  }{
    data => for {
      json <- JsonConversion.objectFromString(data)
      value <- json.getAsObject("value")
    } yield WebsocketUpdate[JsonObject](value)
  }

  def unkwonFromString(data : String): Option[WebsocketFailure] = for {
    json <- JsonConversion.objectFromString(data)
    value <- json.getAsString("reason")
    code <- json.getAsInt("code")
  } yield WebsocketFailure(code, value)

}
