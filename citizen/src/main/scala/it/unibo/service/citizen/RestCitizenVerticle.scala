package it.unibo.service.citizen

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.data.{Data, Storage}
import it.unibo.core.dt.State
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.core.parser.DataParser
import it.unibo.service.authentication.{AuthService, SystemUser}
import it.unibo.service.citizen.RestCitizenVerticle._
import it.unibo.service.citizen.authorization.AuthorizationService
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

    router.get(historyEndpoint)
        .handler(authenticationMiddleware)

    router
  }

  private def handleGetState(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    citizenService.readState(user, citizenIdentifier).onComplete {
      case Success(List()) => context.response().setForbidden() // TODO: forbidden?
      case Success(data) => context.response().setOk(toResource(data))
      case _ => context.response().setInternalError()
    }
  }

  private def handlePatchState(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]

    val requestNewData = context.getBodyAsJson() match {
      case Some(json) => toModel(json)
      case _ => Seq()
    }

    citizenService.updateState(user, citizenIdentifier, requestNewData).onComplete {
      case Success(data) if data.nonEmpty => context.response().setNoContent()
      case Success(_) => context.response().setForbidden()
      case _ => context.response().setInternalError()
    }
  }

  private def toResource(state: Seq[Data]): JsonObject = {
    val json = for { data <- state; encoded <- parser.encode(data) } yield encoded
    Json.obj("data" -> Json.arr(json:_*))
  }

  private def toModel(jsonObject: JsonObject): Seq[Data] = {
    val newData = for { array <- jsonObject.getAsArray("data"); elems <- array.getAsObjectSeq } yield elems
    newData match {
      case Some(data) => for { elem <- data; decoded <- parser.decode(elem) } yield decoded
      case _ => Seq()
    }
  }
}