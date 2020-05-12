package it.unibo.service.citizen.controller

import java.util.UUID

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.RoutingContext
import it.unibo.core.data.{Data, LeafCategory, Storage}
import it.unibo.core.dt.State
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.DataParser
import it.unibo.service.citizen.authorization.AuthorizationFacade

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait CitizenController extends RouterController {
  def handleGetState(context: RoutingContext)
  def handlePatchState(context: RoutingContext)
}

// TODO: need to create a "cold" state starting from Storage.
class DefaultCitizenController(authorizationFacade: AuthorizationFacade,
                               dataStorage : Storage[Data, String],
                               parser : DataParser[JsonObject],
                               private var state: State = State.empty) extends CitizenController {

  override def handleGetState(context: RoutingContext): Unit = {
    filterByAuthorization(state, authorizationFacade, "", "").onComplete {
      case Success(List()) => context.response().setForbidden() // TODO: forbidden?
      case Success(rightData) =>
        val response = buildResponseFromData(rightData)
        context.response().setOk(response)
      case Failure(_) => context.response().setInternalError()
    }
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

  private def buildResponseFromData(dataSeq: Seq[Data]) : JsonObject = {
    val jsonState = for {
      data <- dataSeq
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

  private def filterByAuthorization(state: State, authorizationFacade: AuthorizationFacade, authenticated: String, citizen: String) : Future[Seq[Data]] = {
    // TODO: in real-scenario this is not optimized, if facade do a request at each call. Add interface that accept DataCategory* and return Future[Seq[DataCategory]]
    val readCategories = authorizationFacade.authorizedReadCategories(authenticated, citizen)
    readCategories.map(_.flatMap(state.get))
  }

}