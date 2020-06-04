package it.unibo.covid.demo

import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.SystemUser
import it.unibo.covid.data.{Categories, Parsers}
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.CitizenDigitalTwin
import it.unibo.service.citizen.authentication.MockAuthenticationClient
import it.unibo.service.permission.MockAuthorization
import Categories._
import it.unibo.core.data.{Data, InMemoryStorage, Sensor}
import it.unibo.core.parser.{DataParserRegistry, ValueParser}
import it.unibo.covid.bootstrap.HttpCoapRuntime

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
  val httpPort = args.lift(1).map(_.toInt).getOrElse(8080)
  val coapPort = args.lift(2).map(_.toInt).getOrElse(5683)
  println(s"citizen = $id")
  println(s"HTTP run on $httpPort port")
  println(s"COAP run on $coapPort port")
  val user = SystemUser("foo@foo.it", id, id, id, "citizen")
  val vertx = Vertx.vertx()
  val mockAuth = MockAuthenticationClient(Seq(TokenIdentifier(id) -> user))
  val mockAutho = MockAuthorization(Map((id -> id) -> Seq(locationCategory, medicalDataCategory, personalDataCategory)))
  val citizen = CitizenDigitalTwin.fromVertx(mockAuth, mockAutho, id, InMemoryStorage(), vertx)
  val runtime = new HttpCoapRuntime(httpPort, coapPort, vertx, citizen)
  runtime.start()
}
