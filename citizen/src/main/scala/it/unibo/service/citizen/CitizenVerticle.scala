package it.unibo.service.citizen

import java.util.UUID

import io.vertx.lang.scala.json.Json
import io.vertx.scala.ext.web.Router
import it.unibo.core.data.{Data, Storage}
import it.unibo.core.dt.State
import it.unibo.core.microservice.vertx.BaseVerticle
import it.unibo.core.parser.VertxJsonParser
import it.unibo.core.microservice.vertx._
import CitizenVerticle._
import io.vertx.scala.ext.web.handler.BodyHandler
import it.unibo.service.citizen.controller.CitizenController

import scala.concurrent.Future

object CitizenVerticle {
  private val CITIZEN_ENDPOINT = s"/citizens/%s/state"
}

class CitizenVerticle(controller: CitizenController,
                      citizenIdentifier: String, // could be a UUID or integer, or something else
                      port : Int = 8080,
                      host : String = "localhost") extends BaseVerticle(controller, port, host)  {

  override def createRouter(): Router = {
    val router = Router.router(vertx)

    val citizenStateEndpoint = CITIZEN_ENDPOINT.format(citizenIdentifier)

    router.get(citizenStateEndpoint)
      //.handler(context => authentication...(context))
        .handler(context => controller.handleGetState(context))

    router.patch(citizenStateEndpoint)
        .handler(BodyHandler.create())
        .handler(context => controller.handlePatchState(context))

    router
  }
}