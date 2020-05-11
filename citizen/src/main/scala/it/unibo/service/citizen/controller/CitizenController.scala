package it.unibo.service.citizen.controller

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.data.{Data, DataCategory, DataCategoryOps, LeafCategory, Storage}
import it.unibo.core.dt.State
import it.unibo.core.parser.{DataParser, VertxJsonParser}
import it.unibo.service.citizen.AuthorizationFacade
import it.unibo.core.microservice.vertx._

trait CitizenController {
  def handleGetState(context: RoutingContext)
  def handlePatchState(context: RoutingContext)
}

class DataParserMap[Raw](private val parsers: Seq[DataParser[Raw]]) extends DataParser[Raw] {
  override def decode(rawData: Raw): Option[Data] = parsers.map { p => p.decode(rawData) }.collectFirst {
    case Some(value) => value
  }
  override def encode(data: Data): Option[Raw] = parsers.map { p=> p.encode(data) }.collectFirst {
    case Some(value) => value
  }
  override def target: LeafCategory = LeafCategory("", 0) // TODO: change this
}

// The state act as a cache, the patch handle could manage this aspect.
// E.g. When PATCH request is issued, storage and state need to be updates.
// TODO: if this assumption is ok, we need to create a "cold" state starting from Storage.
class DefaultCitizenController(authorizationFacade: AuthorizationFacade,
                               dataStorage : Storage[Data, String],
                               parser : DataParser[JsonObject],
                               private var state: State = State.empty) extends CitizenController {

  override def handleGetState(context: RoutingContext): Unit = {
    val response = buildResponseFromState(state)
    context.response().setStatusCode(200)
    context.response().end(response.encode())
  }

  override def handlePatchState(context: RoutingContext): Unit = {
    val newData = buildDataFromRequest(context.getBodyAsJson())
    // 1. store new data to storage
    // 2. update state as cache
    newData.foreach {
      data => {
        dataStorage.store(data.id.toString, data)
        state = state.update(data)
      }
    }
    context.response().setStatusCode(200)
    context.response().end()
  }

  private def buildResponseFromState(state: State) : JsonObject = {
    val jsonState = for {
      data <- state.snapshot
      json <- parser.encode(data)
    } yield json
    Json.obj("data" -> Json.arr(jsonState:_*))
  }

  private def buildDataFromRequest(body: Option[JsonObject]): Seq[Data] = {
    val jsonNewData = for {
      obj <- body
      array <- obj.getAsArray("data")
      elems <- array.getAsObjectSeq
    } yield elems

    jsonNewData match {
      case Some(seq) => for {
        elem <- seq
        data <- parser.decode(elem.put("id", UUID.randomUUID().toString))
      } yield data
      case _ => Seq()
    }
  }
}