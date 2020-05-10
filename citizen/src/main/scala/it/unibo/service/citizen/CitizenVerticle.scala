package it.unibo.service.citizen

import io.circe.Json
import io.vertx.scala.ext.web.Router
import it.unibo.core.data.{Data, Storage}
import it.unibo.core.dt.State
import it.unibo.core.microservice.vertx.BaseVerticle
import it.unibo.core.parser.JsonParser

class CitizenVerticle(authorizationFacade: AuthorizationFacade,
                      private var state : State,
                      dataStorage : Storage[Data, String],
                      parsers : Seq[JsonParser],
                      uri : String,
                      port : Int = 8080,
                      host : String = "localhost") extends BaseVerticle(port, host)  {

  override def createRouter(): Router = {
    val router = Router.router(vertx)
    val statePath = s"citizen/$uri/state"

    val getStateRoute = router.get(statePath)
    val updateStateRouter = router.patch(statePath)

    getStateRoute.handler(context => {
      val jsonData = for {
        data <- state.snapshot
        parser <- parsers
        json <- parser.encode(data)
      } yield json
      val response = Json.obj("state" -> Json.arr(jsonData:_*))
      context.response().end(response.toString())
    })

    router
  }


}
