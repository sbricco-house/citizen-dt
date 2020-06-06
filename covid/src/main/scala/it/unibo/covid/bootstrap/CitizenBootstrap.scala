package it.unibo.covid.bootstrap

import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import it.unibo.core.data.{Data, Storage}
import it.unibo.core.microservice.ServiceRuntime
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.parser.ParserLike.MismatchableParser
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.citizen.CitizenDigitalTwin
import it.unibo.service.permission.AuthorizationService

import scala.util.{Failure, Success, Try}
class CitizenBootstrap(authorizationServiceParser : MismatchableParser[JsonObject, AuthorizationService],
                       authenticationServiceParser : MismatchableParser[JsonObject, AuthenticationService],
                       dataRegistryParser: DataParserRegistry[JsonObject],
                       storageParser : MismatchableParser[JsonObject, Storage[Data, String]]) extends ServiceBootstrap[JsonObject] {
  def runtimeFromJson(json : JsonObject) : Try[ServiceRuntime] = {
    val vertx = Vertx.vertx()
    val authorizationTry = tryCreate(json, authorizationServiceParser, "wrong string for authorization")
    val authenticationTry = tryCreate(json, authenticationServiceParser, "wrong string for authentication")
    val storageTry = tryCreate(json, storageParser, "wrong storage option")
    for {
      storage <- storageTry
      authorization <- authorizationTry
      authentication <- authenticationTry
      citizen <- tryCreateCitizen(vertx, json, authentication, authorization, storage)
    } yield createRuntime(json, vertx, citizen, dataRegistryParser)
  }

  private def tryCreate[S](json : JsonObject, parser : MismatchableParser[JsonObject, S], errorString : String) : Try[S] = {
    parser.decode(json)
      .map(service => Success(service))
      .getOrElse(Failure(new IllegalArgumentException(errorString)))
  }

  def tryCreateCitizen(vertx: Vertx,
                       json: JsonObject,
                       authenticationService: AuthenticationService,
                       authorizationService: AuthorizationService,
                       storage: Storage[Data, String]) : Try[CitizenDigitalTwin] = {
    json.getAsString("id")
      .map(CitizenDigitalTwin.fromVertx(authenticationService, authorizationService, _, storage, vertx))
      .map(citizen => Success(citizen))
      .getOrElse(Failure(new IllegalArgumentException("wrong citizen option")))
  }

  def createRuntime(json: JsonObject, vertx: Vertx, citizen : CitizenDigitalTwin, dataParserRegistry: DataParserRegistry[JsonObject]) : ServiceRuntime = {
    val httpPort = json.getAsInt("http_port").getOrElse(8080)
    json.getAsInt("coap_port") match {
      case None => new HttpOnlyRuntime(httpPort, vertx, citizen, dataParserRegistry)
      case Some(coapPort) => new HttpCoapRuntime(httpPort, coapPort, vertx, citizen, dataParserRegistry)
    }
  }
}
