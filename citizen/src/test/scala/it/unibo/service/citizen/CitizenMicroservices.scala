package it.unibo.service.citizen

import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}
import it.unibo.core.data.{Data, GroupCategory, InMemoryStorage, LeafCategory, Storage}
import it.unibo.core.parser.{DataParserRegistry, ValueParser, VertxJsonParser}
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.authentication.mock.MockAuthenticationClient
import it.unibo.service.permission.AuthorizationService
import it.unibo.service.permission.mock.MockAuthorization

object CitizenMicroservices {
  val integerDataParser = VertxJsonParser(ValueParser.Json.intParser, Categories.bloodPressureCategory)
  val doubleDataParser = VertxJsonParser(ValueParser.Json.intParser, Categories.heartBeatCategory)
  val parserRegistry = DataParserRegistry.emptyJson
    .registerParser(integerDataParser)
    .registerParser(doubleDataParser)
    .registerGroupCategory(Categories.medicalDataCategory)

  val authenticationService : AuthenticationService = MockAuthenticationClient(Seq(
    TokenIdentifier("jwt1") -> MockSystemUser("pippo", "50", "citizen"),
    TokenIdentifier("jwt2") -> MockSystemUser("pluto", "47", "stakeholder"),
    TokenIdentifier("jwt3") -> MockSystemUser("paperino", "46", "doctor")
  ))

  val authorizationService : AuthorizationService = MockAuthorization(Map(
    ("jwt1", "50") -> Seq(Categories.medicalDataCategory),
    ("jwt3", "50") -> Seq(Categories.medicalDataCategory, Categories.bloodPressureCategory)
  ))
  private val store : HistoryStorage = HistoryStorage.fromInMemory()

  var citizenService : CitizenDigitalTwin = CitizenDigitalTwin(authenticationService, authorizationService, "50", store)

  def injectVertx(vertx : Vertx) : CitizenDigitalTwin = {
    citizenService = CitizenDigitalTwin.fromVertx(authenticationService, authorizationService, "50", HistoryStorage.fromInMemory(), vertx)
    citizenService
  }

  def refresh(): CitizenDigitalTwin = {
    citizenService = CitizenDigitalTwin(authenticationService, authorizationService, "50", HistoryStorage.fromInMemory())
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


