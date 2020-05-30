package it.unibo.service.citizen

import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.{Data, GroupCategory, InMemoryStorage, LeafCategory, Storage}
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}
import it.unibo.service.authentication.{AuthenticationService, TokenIdentifier}
import it.unibo.service.citizen.authentication.MockAuthenticationClient
import it.unibo.service.permission.{AuthorizationService, MockAuthorization}

object CitizenMicroservices {
  val integerDataParser = VertxJsonParser(ValueParser.Json.intParser, Categories.bloodPressureCategory)
  val doubleDataParser = VertxJsonParser(ValueParser.Json.intParser, Categories.heartBeatCategory)
  val parserRegistry = DataParserRegistry()
    .registerParser(integerDataParser)
    .registerParser(doubleDataParser)
    .registerGroupCategory(Categories.medicalDataCategory)

  val authenticationService : AuthenticationService = MockAuthenticationClient(Seq(
    TokenIdentifier("jwt1") -> MockSystemUser("pippo", "50", "citizen"),
    TokenIdentifier("jwt2") -> MockSystemUser("pluto", "47", "stakeholder"),
    TokenIdentifier("jwt3") -> MockSystemUser("paperino", "46", "doctor")
  ))

  val authorizationService : AuthorizationService = MockAuthorization(Map(
    ("50", "50") -> Seq(Categories.medicalDataCategory),
    ("46", "50") -> Seq(Categories.medicalDataCategory, Categories.bloodPressureCategory)
  ))
  private val store : Storage[Data, String] = InMemoryStorage[Data, String]()

  var citizenService : CitizenDigitalTwin = CitizenDigitalTwin(authenticationService, authorizationService, "50", store)

  def injectVertx(vertx : Vertx) : CitizenDigitalTwin = {
    citizenService = CitizenDigitalTwin.fromVertx(authenticationService, authorizationService, "50", InMemoryStorage[Data, String](), vertx)
    citizenService
  }

  def refresh(): CitizenDigitalTwin = {
    citizenService = CitizenDigitalTwin(authenticationService, authorizationService, "50", InMemoryStorage[Data, String]())
    citizenService
  }
}

object Categories {
  val heartBeatCategory = LeafCategory("heartbeat", 100)
  val bloodPressureCategory = LeafCategory("blood_pressure", 100)
  val medicalDataCategory = GroupCategory("medical", Set(heartBeatCategory, bloodPressureCategory))
}

object MockSystemUser {
  def apply(name: String, identifier: String, role: String): SystemUser = SystemUser("", name, "", identifier, role)
}


