package it.unibo.covid.demo

import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.SystemUser
import it.unibo.covid.data.{Categories, DataParserRegistryParser, Parsers}
import it.unibo.service.authentication.TokenIdentifier
import it.unibo.service.citizen.CitizenDigitalTwin
import it.unibo.service.citizen.authentication.MockAuthenticationClient
import it.unibo.service.permission.MockAuthorization
import Categories._
import io.vertx.core.json.JsonArray
import it.unibo.core.data.InMemoryStorage
import it.unibo.covid.bootstrap.HttpCoapRuntime

import scala.io.Source

object MockDemo extends App {
  val id = args.lift(2).getOrElse("gianluca")
  val httpPort = args.lift(3).map(_.toInt).getOrElse(8080)
  val coapPort = args.lift(4).map(_.toInt).getOrElse(5683)
  val user = SystemUser("foo@foo.it", id, id, id, "citizen")

  val registry = Parsers.configureRegistryFromJson(new JsonArray(Source.fromResource("categories.json").mkString))
  val vertx = Vertx.vertx()
  val mockAuth = MockAuthenticationClient(Seq(TokenIdentifier(id) -> user))
  val mockAutho = MockAuthorization(Map((id -> id) -> Seq(locationCategory, medicalDataCategory, personalDataCategory)))
  val citizen = CitizenDigitalTwin.fromVertx(mockAuth, mockAutho, id, InMemoryStorage(), vertx)
  val runtime = new HttpCoapRuntime(httpPort, coapPort, vertx, citizen, registry)
  runtime.start()
}
