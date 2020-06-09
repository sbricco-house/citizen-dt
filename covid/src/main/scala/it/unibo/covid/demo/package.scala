package it.unibo.covid

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}

import scala.io.Source

package object demo {
  def jsonObjectFromFile(file : String) : JsonObject = Json.fromObjectString(Source.fromResource(file).mkString)
  def jsonArrayFromFile(file: String) : JsonArray = Json.fromArrayString(Source.fromResource(file).mkString)
}
