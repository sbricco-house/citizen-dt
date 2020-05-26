package it.unibo.service.citizen

import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.microservice.vertx.{RestApi, _}
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.utils.{HttpCode, ServiceError}
import it.unibo.core.utils.ServiceError.MissingParameter
import it.unibo.service.citizen.middleware.UserMiddleware

trait RestCitizenApi extends RestApi with RestServiceResponse {
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
    val getOperation = context.queryParams().get(categoryParamName) match {
      case None => citizenService.readState(token)
      case Some(category) => parser.decodeCategory(category)
        .map(citizenService.readStateByCategory(token, _))
        .getOrElse(FutureService.fail(MissingParameter(s"Invalid query value")))
    }

    sendServiceResponseWhenComplete(context, getOperation) {
      case Response(data) => (HttpCode.Ok, stateToJson(data).encode())
    }
  }

  private def handlePatchState(context: RoutingContext): Unit = {
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    val pending = context.getBodyAsJson()
      .map(jsonToState)
      .collect { case Some(data) => data }
      .map(newState => citizenService.updateState(token, newState))
      .getOrElse(FutureService.fail(MissingParameter(s"Invalid json body")))

    sendServiceResponseWhenComplete(context, pending) {
      case Response(newData) => (HttpCode.Ok, seqToJsonArray(newData).encode())
    }
  }

  private def handleGetHistoryData(context: RoutingContext): Unit = {
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    val dataIdentifier = context.pathParam("data_id").get

    sendServiceResponseWhenComplete(context, citizenService.readHistoryData(token, dataIdentifier)) {
      case Response(data) => (HttpCode.Ok, parser.encode(data).get.encode())
    }
  }

  private def handleGetHistoryDataFromCategory(context: RoutingContext): Unit = {
    val token = context.getToken(UserMiddleware.JWT_TOKEN)
    val dataCategory = context.queryParams().get(categoryParamName)
    val limit = context.queryParams().get("limit").getOrElse("1").toInt
    val pending = dataCategory
      .flatMap(parser.decodeCategory)
      .map(citizenService.readHistory(token, _, limit))
      .getOrElse(FutureService.fail(MissingParameter(s"Missing or invalid data_category query parameter")))

    sendServiceResponseWhenComplete(context, pending) {
      case Response(value) => (HttpCode.Ok, dataArrayToJson(value).encode())
    }
  }
}
