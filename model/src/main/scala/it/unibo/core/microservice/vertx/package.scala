package it.unibo.core.microservice

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.core.http.{HttpServerResponse, ServerWebSocket}

package object vertx {
  object JsonConversion {
    def objectFromString(buffer : String) : Option[JsonObject] = tryOrNone(Json.fromObjectString(buffer))

    protected[vertx] def tryOrNone[E](some : => E) : Option[E] = try {
      Some(some)
    } catch {
      case e : Exception => None
    }
  }
  implicit class RichJson(json : JsonObject) {
    import JsonConversion._
    def getAsString(s : String) : Option[String] = if(json.containsKey(s)) {
      tryOrNone { json.getString(s) }
    } else {
      None
    }

    def getAsObject(s : String) : Option[JsonObject] = if(json.containsKey(s)) {
      tryOrNone { json.getJsonObject(s) }
    } else {
      None
    }

    def getAsLong(s : String) : Option[Long] = if(json.containsKey(s)) {
      tryOrNone { json.getLong(s) }
    } else {
      None
    }

    def getAsInt(s : String) : Option[Int] = if(json.containsKey(s)) {
      tryOrNone { json.getInteger(s) }
    } else {
      None
    }

    def getAsBoolean(s : String) : Option[Boolean] = if(json.containsKey(s)) {
      tryOrNone { json.getBoolean(s)}
    } else {
      None
    }

    def getAsArray(s : String) : Option[JsonArray] = if(json.containsKey(s)) {
      tryOrNone { json.getJsonArray(s)}
    } else {
      None
    }

  }

  implicit class RichJsonArray(json : JsonArray) {
    def getAsObjectSeq : Option[Seq[JsonObject]] = {
      val elems = json.size() - 1
      try {
        val objects = (0 to elems).map {
          json.getJsonObject
        }
        Some(objects)
      } catch {
        case exception: Exception => None
      }
    }
  }

  implicit class RichHttpServerResponse(response: HttpServerResponse) {

    private def setResponse(statusCode: Int, obj: JsonObject): Unit = setResponse(statusCode, obj.encode())
    private def setResponse(statusCode: Int, body: String): Unit = {
      response.setStatusCode(statusCode).end(body)
    }

    def setInternalError(message: String = "Internal Error") = setResponse(500, message)
    def setNotFound(message: String = "Not Found") = setResponse(400, message)
    def setForbidden(message: String = "Forbidden") = setResponse(403, message)
    def setNotAuthorized (message: String = "Not authorized") = setResponse(401, message)
    def setBadRequest(message: String = "Bad request") = setResponse(400, message)
    def setConflict(message: String = "Conflict") = setResponse(409, message)
    def setCreated(obj: JsonObject) = setResponse(201, obj)
    def setCreated(obj: String) = setResponse(201, obj)
    def setOk (obj: JsonObject) = setResponse(200, obj)
    def setOk (obj: JsonArray) = setResponse(200, obj.encode())
    def setNoContent() = setResponse(204, "")
  }

  implicit class RichServerWebSocket(webSocket : ServerWebSocket) {
    def rejectInternalError() = webSocket.reject(500)
    def rejectForbidden() = webSocket.reject(403)
    def rejectNotAuthorized() = webSocket.reject(401)
    def rejectBadContent() = webSocket.reject(400)
    def writeTextJsonObject(json : JsonObject) = webSocket.writeTextMessage(json.encode())
    def writeTextJsonArray(json : JsonArray) = webSocket.writeTextMessage(json.encode())
  }
}
