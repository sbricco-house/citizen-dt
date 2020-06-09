package it.unibo.service.permission

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.authentication.middleware.UserMiddleware
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.vertx.{BaseVerticle, RestApi, RestServiceResponse}
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.HttpCode
import it.unibo.core.utils.ServiceError.MissingParameter

/**
 * The http interface to interact with authorization service logic.
 * the endpoints are:
 *  - GET host:/authorization/{citizenId}/write
 *  o OK(200) : [categories..]
 *  # STANDARD_ERRORS
 *
 *  - GET host:/authorization/{citizenId}/write?data_category=x
 *  o OK(200) : [category]
 *  # STANDARD_ERRORS
 *
 *  - GET host:/authorization/{citizenId}/read
 *  o OK(200) : [categories..]
 *  # STANDARD_ERRORS
 *
 *  - GET host:/authorization/{citizenId}/write?data_category=y
 *  o OK(200) : [category]
 *  # STANDARD_ERRORS
 *  # STANDARD_ERRORS
 *  x UNAUTHORIZED(401)
 *  x FORBIDDEN(403)
 *  x INTERNAL_ERROR(500)
 *  x BAD_CONTENT(400)
 *
 * each request must set Authorization header as follow:
 *  Authorization -> Bearer #authToken
 * this implementation is based on vertx.
 * @param authorization The main authorization logic
 * @param parser The parser used to decode/encode categories
 * @param port The http port where the http server starts
 * @param host The host where the http server starts
 */
class AuthorizationVerticle(authorization : AuthorizationService,
                            protected val parser : DataParserRegistry[JsonObject],
                            port : Int = 8080,
                            host : String = "localhost") extends BaseVerticle(port, host) with RestApi with RestServiceResponse {

  private val pathCitizenId = "citizenId"
  private val categoryParamName = "data_category"
  private val authorizationEndpointWrite = s"/authorization/:$pathCitizenId/write"
  private val authorizationEndpointRead = s"/authorization/:$pathCitizenId/read"
  private val options = JWTAuthOptions().addPubSecKey(PubSecKeyOptions().setAlgorithm("HS256").setPublicKey("blabla").setSymmetric(true))

  lazy val provider = JWTAuth.create(vertx, options)
  override def createRouter: Router = {
    val router = Router.router(vertx)
    val userMiddleware = UserMiddleware()

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
    val user = context.get[TokenIdentifier](UserMiddleware.JWT_TOKEN)
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
