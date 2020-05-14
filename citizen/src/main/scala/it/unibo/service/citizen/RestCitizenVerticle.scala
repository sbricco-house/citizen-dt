package it.unibo.service.citizen

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.data.{Data, LeafCategory}
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.core.parser.DataParser
import it.unibo.service.authentication.{AuthService, SystemUser}
import it.unibo.service.citizen.RestCitizenVerticle._
import it.unibo.service.citizen.middleware.AuthMiddleware

import scala.util.{Failure, Success}

object RestCitizenVerticle {
  private val CITIZEN_ENDPOINT = s"/citizens/%s/state"
  private val HISTORY_ENDPOINT = s"/citizens/%s/history"
}

class RestCitizenVerticle(citizenService: CitizenService,
                          parser : DataParser[JsonObject],
                          citizenIdentifier: String, // could be a UUID or integer, or something else
                          port : Int = 8080,
                          host : String = "localhost") extends BaseVerticle(port, host) {

  override def createRouter(): Router = {
    val router = Router.router(vertx)

    val authenticationMiddleware = AuthMiddleware(AuthService())
    val citizenStateEndpoint = CITIZEN_ENDPOINT.format(citizenIdentifier)
    val historyEndpoint = HISTORY_ENDPOINT.format(citizenIdentifier)

    router.get(citizenStateEndpoint)
        .handler(authenticationMiddleware)
        .handler(handleGetState)

    router.patch(citizenStateEndpoint)
        .handler(BodyHandler.create())
        .handler(authenticationMiddleware)
        .handler(handlePatchState)

    router.get(s"$historyEndpoint")
        .handler(authenticationMiddleware)
        .handler(handleGetHistoryDataFromCategory)

    router.get(s"$historyEndpoint/:data_id")
        .handler(authenticationMiddleware)
        .handler(handleGetHistoryData)

    router
  }

  private def handleGetState(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    citizenService.readState(user, citizenIdentifier).onComplete {
      case Success(data) => context.response().setOk(stateToJson(data))
      case _ => context.response().setInternalError()
    }
  }

  private def handlePatchState(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    context.getBodyAsJson().map(jsonToState).map(newData => citizenService.updateState(user, citizenIdentifier, newData)) match {
      case Some(value) => value.onComplete {
        case Success(_) => context.response().setNoContent()
        case Failure(_: IllegalAccessException) => context.response().setForbidden()
        case _ => context.response().setInternalError()
      }
      case None => context.response().setBadRequest()
    }
  }

  private def handleGetHistoryData(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    val dataIdentifier = context.pathParam("data_id").get
    citizenService.readHistoryData(user, citizenIdentifier, dataIdentifier).onComplete {
      case Success(Some(data)) => context.response().setOk(parser.encode(data).get)
      case Success(None) => context.response().setNotFound()
      case Failure(_:IllegalAccessException) => context.response().setForbidden()
      case _ => context.response().setInternalError()
    }
  }

  private def handleGetHistoryDataFromCategory(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    // TODO: datacategory need to be a concrete DataCategory not string, create a placeholder category or registry
    val dataCategory = context.queryParams().get("data_category")
    val limit = context.queryParams().get("limit").getOrElse("1").toInt

    dataCategory.map(LeafCategory(_, -1)).map(category => citizenService.readHistory(user, citizenIdentifier, category, limit)) match {
      case Some(value) => value.onComplete {
        case Success(value) => context.response().setOk(dataArrayToJson(value))
        case Failure(e:IllegalAccessException) => context.response().setForbidden(e.getMessage)
        case Failure(e) => context.response().setInternalError(e.getMessage)
      }
      case None => context.response().setBadRequest()
    }
  }

  // TODO: best way for transform model data to resource response. e.g. using resource mapper for state and history
  private def stateToJson(state: Seq[Data]): JsonObject = {
    Json.obj("data" -> dataArrayToJson(state))
  }

  private def dataArrayToJson(dataSeq: Seq[Data]): JsonArray = {
    val json = for { data <- dataSeq; encoded <- parser.encode(data) } yield encoded
    Json.arr(json:_*)
  }

  private def jsonToState(jsonObject: JsonObject): Seq[Data] = {
    val newData = for { array <- jsonObject.getAsArray("data"); elems <- array.getAsObjectSeq } yield elems
    newData match {
      case Some(data) => for { elem <- data; decoded <- parser.decode(elem) } yield decoded
      case _ => Seq()
    }
  }
}