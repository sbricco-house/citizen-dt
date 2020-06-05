package it.unibo.service.permission

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.microservice.vertx.{BaseVerticle, RestApi, RestServiceResponse}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.HttpCode
import it.unibo.core.utils.ServiceError.MissingParameter

class AuthorizationVerticle(authorization : AuthorizationService,
                            protected val parser : DataParserRegistry[JsonObject],
                            port : Int = 8080,
                            host : String = "localhost") extends BaseVerticle(port, host) with RestApi with RestServiceResponse {

  val pathCitizenId = "citizenId"
  val categoryParamName = "data_category"
  val authorizationEndpointWrite = s"/authorization/:$pathCitizenId/write"
  val authorizationEndpointRead = s"/authorization/:$pathCitizenId/read"

  val provider = JWTAuth.create(vertx, JWTAuthOptions())
  override def createRouter: Router = {
    val router = Router.router(vertx)
    val userMiddleware = UserMiddleware(provider, vertx)

    router.get(authorizationEndpointWrite)
      .handler(userMiddleware)
      .handler(handleGetWriteRead(_, Write))

    router.get(authorizationEndpointRead)
      .handler(userMiddleware)
      .handler(handleGetWriteRead(_, Read))

    router
  }

  sealed trait Mode
  case object Read extends Mode
  case object Write extends Mode

  private def handleGetWriteRead(context : RoutingContext, mode : Mode) : Unit = {
    val user = context.get[SystemUser](UserMiddleware.USER)
    val id = context.pathParam(pathCitizenId).get
    def singleCategory(dataCategory: DataCategory) : FutureService[DataCategory] = mode match {
      case Write => authorization.authorizeWrite(user, id, dataCategory)
      case Read => authorization.authorizeRead(user, id, dataCategory)
    }

    def allCategory() : FutureService[Seq[DataCategory]] = mode match {
      case Read => authorization.authorizedReadCategories(user, id)
      case Write => authorization.authorizedWriteCategories(user, id)
    }

    val getOperation : FutureService[Seq[DataCategory]] = context.queryParams().get(categoryParamName) match {
      case None => allCategory()
      case Some(category) => parser.decodeCategory(category)
        .map(singleCategory)
        .map(service => service.map(Seq(_)))
        .getOrElse(FutureService.fail(MissingParameter(s"Invalid query value")))
    }

    sendServiceResponseWhenComplete(context, getOperation) {
      case Response(data) => (HttpCode.Ok, categoriesToJson(data).encode())
    }
  }
  private def categoriesToJson(categories : Seq[DataCategory]) : JsonArray = {
    Json.arr(categories.map(_.name):_*)
  }
}
