package it.unibo.service.citizen.websocket

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.microservice.protocol.{WebsocketFailure, WebsocketRequest, WebsocketResponse, WebsocketUpdate}
import it.unibo.core.microservice.vertx.{JsonConversion, _}
import it.unibo.core.parser.ParserLike

/**
 * a set of message and parser used by websocket to build an upper protocol to manage different
 * type of communications (e.g. request and response).
 */
object CitizenProtocol {
  import Status._

  /**
   * the response when the client speaks other "protocol" .
   */
  val unknown : JsonObject = Json.obj("reason" -> "unknown protocol", "code" -> 500)
  /**
   * in the request, some data category are unknown by the server
   */
  val unknownDataCategoryError : Failed = Failed("unknown data")
  /**
   * some internal operations (e.g. citizen dt updates) go wrong.
   */
  val internalError : Failed = Failed("internal error")
  /**
   * the operations requested can be satisfied due to permission problems linked to data category
   */
  val unauthorized : Failed = Failed("category not allowed")

  val responseParser = ParserLike[String, WebsocketResponse[Status]] {
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

  val requestParser = ParserLike[String, WebsocketRequest[JsonArray]] {
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

  val updateParser = ParserLike[String, WebsocketUpdate[JsonObject]]{
    rep => Json.obj(
      "value" -> rep.value
    ).toString
  }{
    data => for {
      json <- JsonConversion.objectFromString(data)
      value <- json.getAsObject("value")
      if Status.fromJson(value).isEmpty
    } yield WebsocketUpdate[JsonObject](value)
  }
}
