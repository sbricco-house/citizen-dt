package it.unibo.service.citizen

import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.microservice.vertx.{RestApi, _}
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.protocol.ServiceError.{BadParameter, MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.citizen.middleware.UserMiddleware

trait RestCitizenApi extends RestApi {
  self : RestCitizenVerticle =>
  import RestCitizenVerticle._

  override def createRouter: Router = {
    val router = Router.router(vertx)
    val userMiddleware = UserMiddleware()

    router.get(self.citizenStateEndpoint)
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
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    citizenService.readState(token, citizenIdentifier).whenComplete {
      case Response(data) => context.response().setOk(stateToJson(data))
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handlePatchState(context: RoutingContext): Unit = {
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    val pending = context.getBodyAsJson()
      .map(jsonToState)
      .map(newState => citizenService.updateState(token, citizenIdentifier, newState))
      .getOrElse(FutureService.fail(BadParameter(s"Invalid json body")))

    pending.whenComplete {
      case Response(newData) => context.response().setCreated(stateToJson(newData))
      case Fail(MissingParameter(m)) => context.response().setBadRequest(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handleGetHistoryData(context: RoutingContext): Unit = {
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    val dataIdentifier = context.pathParam("data_id").get

    citizenService.readHistoryData(token, citizenIdentifier, dataIdentifier).whenComplete {
      case Response(data) => context.response().setOk(parser.encode(data).get)
      case Fail(MissingResource(m)) => context.response().setNotFound(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }

  private def handleGetHistoryDataFromCategory(context: RoutingContext): Unit = {
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    val dataCategory = context.queryParams().get("data_category")
    val limit = context.queryParams().get("limit").getOrElse("1").toInt
    val pending = dataCategory
      .flatMap(dataCategoryRegistry.get)
      .map(citizenService.readHistory(token, citizenIdentifier, _, limit))
      .getOrElse(FutureService.fail(BadParameter(s"Missing or invalid data_category query parameter")))

    pending.whenComplete {
      case Response(value) => context.response().setOk(dataArrayToJson(value))
      case Fail(BadParameter(m)) => context.response().setBadRequest(m)
      case Fail(Unauthorized(m)) => context.response().setForbidden(m)
      case _ => context.response().setInternalError()
    }
  }
}
