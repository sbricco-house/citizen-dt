package it.unibo.service.citizen

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Router, RoutingContext}
import it.unibo.core.data.{Data, DataCategory, Storage}
import it.unibo.core.dt.State
import it.unibo.core.microservice.vertx.{BaseVerticle, _}
import it.unibo.core.parser.DataParser
import it.unibo.service.citizen.CitizenVerticle._
import it.unibo.service.citizen.authorization.AuthorizationFacade
import it.unibo.service.citizen.middleware.AuthMiddleware

import scala.concurrent.Future
import scala.util.{Failure, Success}

object CitizenVerticle {
  private val CITIZEN_ENDPOINT = s"/citizens/%s/state"
}

class CitizenVerticle(authorizationFacade: AuthorizationFacade,
                      dataStorage : Storage[Data, String],
                      parser : DataParser[JsonObject],
                      citizenIdentifier: String, // could be a UUID or integer, or something else
                      private var state: State = State.empty,
                      port : Int = 8080,
                      host : String = "localhost") extends BaseVerticle(port, host)  {

  override def createRouter(): Router = {
    val router = Router.router(vertx)

    val authenticationMiddleware = AuthMiddleware.create(AuthService())
    val citizenStateEndpoint = CITIZEN_ENDPOINT.format(citizenIdentifier)

    router.get(citizenStateEndpoint)
        .handler(authenticationMiddleware)
        .handler(handleGetState)

    router.patch(citizenStateEndpoint)
        .handler(BodyHandler.create())
        .handler(authenticationMiddleware)
        .handler(handlePatchState)

    router
  }

  private def handleGetState(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    val authorizedCategories = authorizationFacade.authorizedReadCategories(user.identifier, citizenIdentifier)

    authorizedCategories.onComplete {
      case Success(List()) => context.response().setForbidden() // TODO: forbidden?
      case Success(categories) => context.response().setOk(buildResponseFromData(state.get(categories)))
      case Failure(_) => context.response().setInternalError()
    }
  }

  private def handlePatchState(context: RoutingContext): Unit = {
    val user = context.get(AuthMiddleware.AUTHENTICATED_USER).asInstanceOf[SystemUser]
    val authorizedCategories = authorizationFacade.authorizedWriteCategories(user.identifier, citizenIdentifier)
    val requestNewData = buildDataFromRequest(context.getBodyAsJson())
    val requestCategories = requestNewData.map(_.category)

    authorizedCategories.onComplete {
      case Success(categories) if categories == requestCategories =>
        saveNewData(requestNewData)
        context.response().setNoContent()
      case Failure(_) => context.response().setInternalError()
      case _ => context.response().setForbidden()
    }
  }

  private def saveNewData(data: Seq[Data]): Unit = {
    data.foreach(d => {
      dataStorage.store(d.id.toString, d)
      state = state.update(d)
    })
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

  private def buildResponseFromData(dataSeq: Seq[Data]) : JsonObject = {
    val jsonState = for {
      data <- dataSeq
      json <- parser.encode(data)
    } yield json
    Json.obj("data" -> Json.arr(jsonState:_*))
  }
}