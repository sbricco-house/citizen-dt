package it.unibo.covid.demo

import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}
import it.unibo.core.data.InMemoryStorage
import it.unibo.core.microservice.vertx._
import it.unibo.covid.bootstrap.HttpCoapRuntime
import it.unibo.covid.data.Parsers
import it.unibo.service.authentication.mock.MockAuthenticationClient
import it.unibo.service.citizen.{CitizenDigitalTwin, HistoryStorage}
import it.unibo.service.permission.mock.MockAuthorization

/**
 * a demo in which only citizen system has its runtime.
 * the main accept three argument:
 *  id httpPort coapPort
 * the default value are respectively "gianluca" "8080" "5683"
 * to make request, in the authorization field you must put id value.
 *
 * the path of http request is : http://localhost:{httpPort}/citizens/{id}/state
 */
object MockDemo extends App {
  val id = args.headOption.getOrElse("gianluca")
  val token = TokenIdentifier(id)
  val httpPort = args.lift(1).map(_.toInt).getOrElse(8080)
  val coapPort = args.lift(2).map(_.toInt).getOrElse(5683)
  println(s"citizen = $id")
  println(s"HTTP run on $httpPort port")
  println(s"COAP run on $coapPort port")

  JsonConversion.objectFromString("ciao")
  val user = SystemUser("foo@foo.it", id, id, id, "citizen")

  val registry = Parsers.configureRegistry()
  val vertx = Vertx.vertx()
  val mockAuth = MockAuthenticationClient(Seq(token -> user))
  val mockAutho = MockAuthorization.acceptAll(registry)
  val citizen = CitizenDigitalTwin.fromVertx(mockAuth, mockAutho, id, HistoryStorage.fromInMemory(), vertx)
  val runtime = new HttpCoapRuntime(httpPort, coapPort, vertx, citizen, registry)
  runtime.start()

}
