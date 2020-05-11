package it.unibo.core.microservice

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}

import scala.util.Try

package object vertx {
  implicit class RichJson(json : JsonObject) {
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
    private def tryOrNone[E](some : => E) : Option[E] = try {
      Some(some)
    } catch {
      case e : Exception => None
    }
  }
  implicit class RichJsonArray(json : JsonArray) {
    import collection.JavaConverters._
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
}
