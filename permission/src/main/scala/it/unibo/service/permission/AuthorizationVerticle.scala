package it.unibo.service.permission

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.ext.web.Router
import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.vertx.{BaseVerticle, RestApi}
import it.unibo.core.parser.DataParserRegistry

class AuthorizationVerticle(authorization : AuthorizationService,
                            protected val parser : DataParserRegistry[JsonObject],
                            port : Int = 8080,
                            host : String = "localhost") extends BaseVerticle(port, host) with RestApi {
  "/authorization/{id}/state/"

  override def createRouter: Router = {
    val router = Router.router(vertx)
  /*  val userMiddleware = UserMiddleware()

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
      .handler(handleGetHistoryData)*/
    SystemUser
    router
  }
}
