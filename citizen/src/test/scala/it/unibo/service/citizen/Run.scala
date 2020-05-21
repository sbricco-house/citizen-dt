package it.unibo.service.citizen

import java.net.URI

import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import io.vertx.scala.servicediscovery.ServiceDiscovery
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.service.authentication.app.UserStorage
import it.unibo.service.authentication.{AuthenticationService, AuthenticationVerticle, RestAuthenticationApi}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

object Run extends App {
  val userStorage = UserStorage.generateDefault()

  val vertx = Vertx.vertx()
  val options = JWTAuthOptions()
    .setPubSecKeys(mutable.Buffer(PubSecKeyOptions()
      .setAlgorithm("HS256")
      .setPublicKey("keyboard cat")
      .setSymmetric(true)))

  val provider = JWTAuth.create(vertx, options)
  val auth = AuthenticationService(provider, userStorage)
  val verticle = new AuthenticationVerticle(auth, 8081, "localhost") with RestAuthenticationApi

  Await.result(vertx.deployVerticleFuture(verticle), 5 seconds)


  val discovery = ServiceDiscovery.create(vertx)

  discovery.getRecordFuture(record => record.getName == "authentication").onComplete {
    case Failure(exception) =>
    case Success(Some(record)) => {
      val authenticationService = AuthenticationService.createProxy(URI.create(record.getLocation.getString("endpoint")))
      authenticationService.login("citizen1@email.com", "citizen1").whenComplete {
        case Response(content) => println(s"Token $content")
        case Fail(error) => println(s"Error during login: $error")
      }(ExecutionContext.global)
    }
  }(ExecutionContext.global)
}
