package it.unibo.service.citizen

import java.util.UUID

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.data.Data
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.core.parser.DataParser
import it.unibo.core.registry.DataCategoryRegistry
import it.unibo.service.authentication.TokenIdentifier

object RestCitizenVerticle {
  private val CITIZEN_ENDPOINT = s"/citizens/%s/state"
  private val HISTORY_ENDPOINT = s"/citizens/%s/history"

  implicit class RichContext(contet: RoutingContext) {
    def getToken(key: String): TokenIdentifier = contet.get[TokenIdentifier](key)
  }
}

class RestCitizenVerticle(protected val citizenService: CitizenService,
                          protected val parser : DataParser[JsonObject],
                          protected val dataCategoryRegistry: DataCategoryRegistry,
                          protected val citizenIdentifier: String, // could be a UUID or integer, or something else
                          port : Int = 8080,
                          host : String = "localhost") extends BaseVerticle(port, host) {

  import RestCitizenVerticle._
  protected val citizenStateEndpoint = CITIZEN_ENDPOINT.format(citizenIdentifier)
  protected val historyEndpoint = HISTORY_ENDPOINT.format(citizenIdentifier)

  // TODO: best way for transform model data to resource response. e.g. using resource mapper for state and history
  protected def stateToJson(state: Seq[Data]): JsonObject = {
    Json.obj("data" -> dataArrayToJson(state))
  }

  protected def dataArrayToJson(dataSeq: Seq[Data]): JsonArray = {
    Json.arr(dataSeq.flatMap(parser.encode):_*)
  }

  // TODO: what do if an update state (patch) contains unspported datacategory? or wrong format of supported category?
  // actually only valid data are parsed and returned
  protected def jsonToState(jsonObject: JsonObject): Seq[Data] = {
    jsonObject.getAsArray("data")
      .flatMap(_.getAsObjectSeq)
      .map(json => json.map(_.put("id", UUID.randomUUID().toString))) // assign unique data identifier
      .map(jsonData => jsonData.flatMap(parser.decode))
      .getOrElse(Seq())
  }
}