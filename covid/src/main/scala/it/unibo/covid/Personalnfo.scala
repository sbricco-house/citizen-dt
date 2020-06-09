package it.unibo.covid

import java.util.UUID

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, LeafCategory, Resource}
import it.unibo.core.microservice.vertx._

/**
 * utility function used to load a system user from json file.
 * the json must have:
 * {
 *    "name": ..
 *    "surname" : ..
 *    "birthdate" : ..
 *    "cf" : ..
 * }
 */
object Personalnfo {
  def fromJson(json: JsonObject): Seq[Data] = {
    val personalInfo = for {
      name <- json.getAsString("name").flatMap(Personalnfo.toData("name", _))
      surname <- json.getAsString("surname").flatMap(Personalnfo.toData("surname", _))
      birthDate <- json.getAsString("birthdate").flatMap(Personalnfo.toData("birthdate", _))
      cf <- json.getAsString("cf").flatMap(Personalnfo.toData("cf", _))
    } yield Seq(name, surname, birthDate, cf)
    personalInfo.getOrElse(Seq())
  }

  private def toData(category: String, value: Any): Option[Data] = {
    val identifier = UUID.randomUUID().toString
    val feeder = Resource("")
    Some(Data(identifier, feeder, LeafCategory(category, -1), -1, value))
  }
}