package it.unibo.service.citizen.authentication

import java.net.URI

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.{Json, JsonObject}
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.client.{WebClient, WebClientOptions}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.client.RestApiClient
import it.unibo.core.microservice.vertx._
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.service.authentication.{AuthenticationService, TokenIdentifier}
import it.unibo.service.citizen.authentication.AuthenticationClient._
import it.unibo.core.client._

object AuthenticationClient {
  val VERIFY = s"/verify?token=%s"
}

class AuthenticationClient(serviceUri: URI) extends AuthenticationService with RestApiClient {

  private val clientOptions =  WebClientOptions()
    .setFollowRedirects(true)

  private val vertx = Vertx.vertx()
  private val client: WebClient = WebClient.create(vertx, clientOptions)
  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def login(email: String, password: String): FutureService[TokenIdentifier] = ???

  override def getAuthenticatedUser(identifier: TokenIdentifier): FutureService[SystemUser] = {
    val request = s"${serviceUri.toString}$VERIFY".format(identifier.token)
    client.get(request).sendFuture()
      .map(response => response.mapToServiceResponse {
        case (200, body) => Response(parseLoginUser(Json.fromObjectString(body)).get)
      }).toFutureService
  }

  override def refresh(authenticated: SystemUser): FutureService[SystemUser] = ???

  override def logout(identifier: TokenIdentifier): FutureService[Boolean] = ???

  protected def parseLoginUser(jsonObject: JsonObject): Option[SystemUser] = {
    val emailOption = jsonObject.getAsString("email")
    val username = jsonObject.getAsString("username")
    val passwordOption = jsonObject.getAsString("password")
    val identifierOption = jsonObject.getAsString("identifier")
    val roleOption = jsonObject.getAsString("role")
    for {
      email <- emailOption
      password <- passwordOption
    } yield SystemUser(email, username.getOrElse(""), password, identifierOption.getOrElse(""), roleOption.getOrElse(""))
  }
}

/*
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
*/