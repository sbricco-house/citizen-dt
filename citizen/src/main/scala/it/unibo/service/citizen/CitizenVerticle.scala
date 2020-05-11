package it.unibo.service.citizen

import java.util.UUID

import io.vertx.lang.scala.json.Json
import io.vertx.scala.ext.web.Router
import it.unibo.core.data.{Data, Storage}
import it.unibo.core.dt.State
import it.unibo.core.microservice.vertx.BaseVerticle
import it.unibo.core.parser.VertxJsonParser
import it.unibo.core.microservice.vertx._
class CitizenVerticle(authorizationFacade: AuthorizationFacade,
                      private var state : State,
                      dataStorage : Storage[Data, String],
                      parsers : Seq[VertxJsonParser],
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

      val response = Json.arr(jsonData:_*)
      context.response().setStatusCode(200)
      context.response().end(response.encode())
    })
    import io.vertx.scala.core.json._
    updateStateRouter.handler(context => {
      val jsonArray = for {
        body <- context.getBodyAsJson()
        array <- body.getAsArray("state")
        elems <- array.getAsObjectSeq
      } yield elems

      val unmarshalData = jsonArray match {
        case Some(seq) => for {
          elem <- seq
          parser <- parsers
          data <- parser.decode(elem, UUID.randomUUID().toString)
        } yield data
        case _ => Seq() //TODO
      }

      unmarshalData.foreach { data => state = state.update(data)}
      context.response().setStatusCode(200)
      context.response().end()
    })
    router
  }
}