package it.unibo.service.citizen

import io.vertx.core.json.JsonArray
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.data.{Data, LeafCategory}
import it.unibo.core.microservice._
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.core.parser.DataParser
import it.unibo.core.registry.DataCategoryRegistry
import it.unibo.core.utils.ServiceError.{BadParameter, MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.citizen.middleware.UserMiddleware

object RestCitizenVerticle {
  private val CITIZEN_ENDPOINT = s"/citizens/%s/state"
  private val HISTORY_ENDPOINT = s"/citizens/%s/history"

  implicit class RichContext(contet: RoutingContext) {
    def getString(key: String): String = contet.get[String](key)
  }
}

class RestCitizenVerticle(citizenService: CitizenService,
                          parser : DataParser[JsonObject],
                          dataCategoryRegistry: DataCategoryRegistry,
                          citizenIdentifier: String, // could be a UUID or integer, or something else
                          port : Int = 8080,
                          host : String = "localhost") extends BaseVerticle(port, host) {

  import RestCitizenVerticle._
  private val citizenStateEndpoint = CITIZEN_ENDPOINT.format(citizenIdentifier)
  private val historyEndpoint = HISTORY_ENDPOINT.format(citizenIdentifier)

  override def createRouter(): Router = {
    val router = Router.router(vertx)
    val userMiddleware = UserMiddleware()

    router.get(citizenStateEndpoint)
        .handler(userMiddleware)
        .handler(handleGetState)

    router.patch(citizenStateEndpoint)
        .handler(BodyHandler.create())
        .handler(userMiddleware)
        .handler(handlePatchState)

    router.get(s"$historyEndpoint")
        .handler(userMiddleware)
        .handler(handleGetHistoryDataFromCategory)

    router.get(s"$historyEndpoint/:data_id")
        .handler(userMiddleware)
        .handler(handleGetHistoryData)

    router
  }

  private def handleGetState(context: RoutingContext): Unit = {
    val user = context.getString(UserMiddleware.AUTHENTICATED_USER)

    citizenService.readState(user, citizenIdentifier).whenComplete {
      case Response(data) => context.response().setOk(stateToJson(data))
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handlePatchState(context: RoutingContext): Unit = {
    val user = context.getString(UserMiddleware.AUTHENTICATED_USER)
    val pending = context.getBodyAsJson()
      .map(jsonToState)
      .map(newState => citizenService.updateState(user, citizenIdentifier, newState))
      .getOrElse(FutureService.fail(BadParameter(s"Invalid json body")))

    pending.whenComplete {
      case Response(_) => context.response().setNoContent()
      case Fail(MissingParameter(m)) => context.response().setBadRequest(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handleGetHistoryData(context: RoutingContext): Unit = {
    val user = context.getString(UserMiddleware.AUTHENTICATED_USER)
    val dataIdentifier = context.pathParam("data_id").get

    citizenService.readHistoryData(user, citizenIdentifier, dataIdentifier).whenComplete {
      case Response(data) => context.response().setOk(parser.encode(data).get)
      case Fail(MissingResource(m)) => context.response().setNotFound(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handleGetHistoryDataFromCategory(context: RoutingContext): Unit = {
    val user = context.getString(UserMiddleware.AUTHENTICATED_USER)
    val dataCategory = context.queryParams().get("data_category")
    val limit = context.queryParams().get("limit").getOrElse("1").toInt
    val pending = dataCategory
        .flatMap(dataCategoryRegistry.get)
        .map(citizenService.readHistory(user, citizenIdentifier, _, limit))
        .getOrElse(FutureService.fail(BadParameter(s"Missing or invalid data_category query parameter")))

    pending.whenComplete {
      case Response(value) => context.response().setOk(dataArrayToJson(value))
      case Fail(BadParameter(m)) => context.response().setBadRequest(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  // TODO: best way for transform model data to resource response. e.g. using resource mapper for state and history
  private def stateToJson(state: Seq[Data]): JsonObject = {
    Json.obj("data" -> dataArrayToJson(state))
  }

  private def dataArrayToJson(dataSeq: Seq[Data]): JsonArray = {
    Json.arr(dataSeq.flatMap(parser.encode):_*)
  }

  private def jsonToState(jsonObject: JsonObject): Seq[Data] = {
    jsonObject.getAsArray("data")
      .flatMap(_.getAsObjectSeq)
      .map(jsonData => jsonData.flatMap(parser.decode))
      .getOrElse(Seq())
  }
}