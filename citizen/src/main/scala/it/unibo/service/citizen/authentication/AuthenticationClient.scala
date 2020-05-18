package it.unibo.service.citizen.authentication

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.client.WebClient
import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.citizen.authentication.AuthenticationClient._

import scala.concurrent.ExecutionContext

object AuthenticationClient {
  val VERIFY = s"/verify"
}

class AuthenticationClient(serviceUri: String) extends AuthenticationService {

  private val vertx = Vertx.vertx()
  private val client = WebClient.create(vertx)
  private implicit val executionContext: ExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def getAuthenticatedUser(identifier: String): FutureService[SystemUser] = {
    client.get(s"$serviceUri$VERIFY").sendFuture().map(r => (r.statusCode(), r.body())).map {
      case (200, buffer) => Response(jsonToSystemUser(buffer.toString))
      case _ => Fail(Unauthorized())
    }.toFutureService
  }

  private def jsonToSystemUser(json: String): SystemUser = {
    val obj = new JsonObject(json)
    SystemUser(obj.getString("identifier"), obj.getString("role"))
  }
}
