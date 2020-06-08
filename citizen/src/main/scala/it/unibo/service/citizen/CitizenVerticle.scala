package it.unibo.service.citizen

import java.util.UUID

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.data.Data
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.core.parser.DataParserRegistry

object CitizenVerticle {
  private val CITIZEN_ENDPOINT = s"/citizens/%s/state"
  private val HISTORY_ENDPOINT = s"/citizens/%s/history"

  implicit class RichContext(contet: RoutingContext) {
    def getToken(key: String): TokenIdentifier = contet.get[TokenIdentifier](key)
  }
}

/**
 * describe the context (in terms of http vertx platform) in which citizen logics are involved.
 * @param citizenDT The logic to manage a citizen (e.g. state update, watching data,...)
 * @param parser The object used to marshall and unmarshal data
 * @param port The port in which the http server starts
 * @param host The host in which the http server starts
 */
class CitizenVerticle(protected val citizenDT: CitizenDigitalTwin,
                      protected val parser : DataParserRegistry[JsonObject],
                      port : Int = 8080,
                      host : String = "localhost") extends BaseVerticle(port, host) {

  import CitizenVerticle._
  //some elements used by possible other decorations (e.g. websocket, http rest,..)
  protected val citizenStateEndpoint = CITIZEN_ENDPOINT.format(citizenDT.citizenIdentifier)
  protected val historyEndpoint = HISTORY_ENDPOINT.format(citizenDT.citizenIdentifier)
  protected val categoryParamName = "data_category"
  // TODO: best way for transform model data to resource response. e.g. using resource mapper for state and history
  protected def stateToJson(state: Seq[Data]): JsonObject = {
    Json.obj("data" -> dataArrayToJson(state))
  }

  protected def dataArrayToJson(dataSeq: Seq[Data]): JsonArray = {
    Json.arr(dataSeq.flatMap(parser.encode):_*)
  }

  protected def seqToJsonArray[E](elements : Seq[String]) :JsonArray = Json.arr(elements:_*)

  protected def jsonToState(jsonObject: JsonObject): Option[Seq[Data]] = jsonObject.getAsArray("data").flatMap(jsonArrayToData)

  protected def jsonArrayToData(array: JsonArray): Option[Seq[Data]] = {
    array.getAsObjectSeq
      .map(json => json.map(_.put("id", UUID.randomUUID().toString))) // assign unique data identifier
      .map(jsonData => jsonData.map(parser.decode))
      .filter(elements => elements.forall(_.nonEmpty))
      .map(elems => elems.map(_.get))
  }
}