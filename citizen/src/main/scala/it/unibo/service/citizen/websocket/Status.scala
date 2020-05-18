package it.unibo.service.citizen.websocket

import io.vertx.lang.scala.json.{Json, JsonObject}

sealed trait Status

case object Ok extends Status

case class Failed(reason : String) extends Status

object Status {
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
