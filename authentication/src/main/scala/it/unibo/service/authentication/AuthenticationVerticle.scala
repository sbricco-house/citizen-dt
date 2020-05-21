package it.unibo.service.authentication

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.servicediscovery.ServiceDiscovery
import io.vertx.scala.servicediscovery.types.HttpEndpoint
import it.unibo.core.authentication.SystemUser
import it.unibo.core.microservice.vertx.{BaseVerticle, _}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class AuthenticationVerticle(protected val authenticationService: AuthenticationService,
                             port : Int = 8080,
                             host : String = "0.0.0.0") extends BaseVerticle(port, host) {

  case class LoginUser(email: String, password: String)

  override def startFuture(): Future[Unit] = {
    val discovery = ServiceDiscovery.create(vertx)
    val jsonApi = new JsonObject().put("api.name", s"authentication")
    val record = HttpEndpoint.createRecord(s"authentication", host, port, "/", jsonApi)
    discovery.publishFuture(record).onComplete {
      case Failure(exception) => println(s"Error during service registration! ${exception.getMessage}")
      case Success(value) => println(s"Service registered! ${value.getName}")
    }
    super.startFuture().flatMap(_ => Future.successful())
  }

  protected def parseLoginUser(jsonObject: JsonObject): Option[LoginUser] = {
    val emailOption = jsonObject.getAsString("email")
    val passwordOption = jsonObject.getAsString("password")
    for {
      email <- emailOption
      password <- passwordOption
    } yield LoginUser(email, password)
  }

  protected def userToJson(user: SystemUser): JsonObject = {
    new JsonObject()
      .put("email", user.email)
      .put("username", user.username)
      .put("identifier", user.identifier)
      .put("role", user.role)
  }
}
