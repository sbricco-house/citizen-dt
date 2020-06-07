package it.unibo.service.citizen.websocket

import io.vertx.lang.scala.json.{Json, JsonObject}

/**
 * an application protocol part for websocket communication.
 * Describe the result of a request
 */
sealed trait Status

/**
 * the operation has been complete successfully
 */
case object Ok extends Status

/**
 * the operation fail. This object contains the reason of the failure.
 * @param reason The reason associated to this operation fail.
 */
case class Failed(reason : String) extends Status
//TODO fix, Ok must report the id of data
object Status {
  //some utilities for status management
  import it.unibo.core.microservice.vertx._

  implicit class RichStatus(s : Status) {
    def toJson : JsonObject = s match {
      case Ok => Json.obj("status" -> "ok")
      case Failed(r) => Json.obj("status" -> "failed", "reason" -> r)
    }
  }

  def fromJson(obj : JsonObject) : Option[Status] = obj.getAsString("status").flatMap {
    case "ok" => Some(Ok)
    case "failed" => obj.getAsString("reason").map(reason => Failed(reason))
    case _ => None
  }
}
