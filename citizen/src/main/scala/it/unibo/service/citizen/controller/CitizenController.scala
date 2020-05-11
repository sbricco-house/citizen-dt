package it.unibo.service.citizen.controller

import java.util.UUID

import io.circe.JsonObject
import io.vertx.lang.scala.json.Json
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.data.{Data, Storage}
import it.unibo.core.dt.State
import it.unibo.core.parser.VertxJsonParser
import it.unibo.service.citizen.AuthorizationFacade
import it.unibo.core.microservice.vertx._

trait CitizenController {
  def handleGetState(context: RoutingContext)
  def handlePatchState(context: RoutingContext)
}

// The state act as a cache, the patch handle could manage this aspect.
// E.g. When PATCH request is issued, storage and state need to be updates.
// TODO: if this assumption is ok, we need to create a "cold" state starting from Storage.
class DefaultCitizenController(authorizationFacade: AuthorizationFacade,
                               dataStorage : Storage[Data, String],
                               parsers : Seq[VertxJsonParser],
                               private var state: State = State.empty) extends CitizenController {

  override def handleGetState(context: RoutingContext): Unit = {
    val jsonState = for {
      data <- state.snapshot
      parser <- parsers
      json <- parser.encode(data)
    } yield json

    val response = Json.obj("data" -> Json.arr(jsonState:_*))
    context.response().setStatusCode(200)
    context.response().end(response.encode())
  }

  override def handlePatchState(context: RoutingContext): Unit = {
    val jsonNewData = for {
      jsonObj <- context.getBodyAsJson()
      array <- jsonObj.getAsArray("data")
      elems <- array.getAsObjectSeq
    } yield elems

    val decodedData = jsonNewData match {
      case Some(seq) => for {
        elem <- seq
        parser <- parsers
        data <- parser.decode(elem)
      } yield data
      case _ => Seq()
    }

    // 1. store new data to storage
    // 2. update state as cache
    decodedData.foreach {
      data => {
        dataStorage.store(UUID.randomUUID().toString, data) // UUID inside implementation ?
        state = state.update(data)
      }
    }
    context.response().setStatusCode(200)
    context.response().end()
  }
}