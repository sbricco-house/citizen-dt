package it.unibo.covid.demo

import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, InMemoryStorage, Storage}
import it.unibo.core.parser.ParserLike
import it.unibo.covid.bootstrap.CitizenBootstrap
import it.unibo.covid.data.Parsers
import it.unibo.covid.demo.ClientParsers._
import it.unibo.service.citizen.HistoryStorage

import scala.util.{Failure, Success}

object DemoFromJson extends App {
  val json = args.headOption.orElse(Some("default-citizen.json")).map(jsonObjectFromFile).get
  val registry = args.lift(1) match {
    case None => Parsers.configureRegistry()
    case Some(file) => Parsers.configureRegistryFromJson(jsonArrayFromFile(file))
  }

  private val storageParser = ParserLike.decodeOnly[JsonObject, HistoryStorage] {
    (json : JsonObject) => Some(HistoryStorage.fromInMemory())
  }
  val bootstrapper = new CitizenBootstrap(authorizationParser, authenticationParser, registry, storageParser)

  bootstrapper.runtimeFromJson(json) match {
    case Success(runtime) => runtime.start()
    case Failure(exception) => println(exception)
  }
}
