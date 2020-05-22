package it.unibo.service.citizen

import java.util.concurrent.Executors

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.core.Vertx
import io.vertx.scala.core.Vertx.vertx
import it.unibo.core.data._
import it.unibo.core.dt.History.History
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.{AuthenticationService, TokenIdentifier}
import it.unibo.service.permission.AuthorizationService
import monix.reactive.Observable

import scala.concurrent.ExecutionContext

/**
 * Abstraction of Citizen Service expressed using main domain concept.
 * key idea: allow to expose the same service through different web technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: TokenIdentifier, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]]
  def readState(who: TokenIdentifier, citizenId: String): FutureService[Seq[Data]]
  def readHistory(who: TokenIdentifier, citizenId: String, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]
  def readHistoryData(who: TokenIdentifier, citizenId: String, dataIdentifier: String): FutureService[Data]
  // TODO: define better using rx scala
  def observeState(who: TokenIdentifier, citizenId: String): FutureService[Channel]

  trait Channel {
    def updateState(data: Seq[Data]): FutureService[Seq[Data]]
    def updateDataStream() : Observable[Data]
    def close() : Unit
  }

  protected trait SourceChannel extends Channel {
    def emit(data : Seq[Data]) : Unit
  }
}

object CitizenService {

  /**
   * Create the default BackendService
   * @param authenticationService Service that provide authentication
   * @param authorizationService Service that provide authorization check
   * @param dataStorage Storage where save the citizen data
   * @return A Citizen Service backend instance
   */
  def apply(authenticationService : AuthenticationService,
            authorizationService: AuthorizationService,
            dataStorage: Storage[Data, String]): CitizenService = {
    implicit val execution = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    new BackendCitizenService(authenticationService, authorizationService, dataStorage)
  }

  def fromVertx(authenticationService : AuthenticationService,
            authorizationService: AuthorizationService,
            dataStorage: Storage[Data, String],
            vertx: Vertx): CitizenService = {
    implicit val execution = VertxExecutionContext(vertx.getOrCreateContext())
    new BackendCitizenService(authenticationService, authorizationService, dataStorage)
  }

  // the same interface could be used for create the client counterpart. Useful for test the backend.
  // e.g. a client could use vertx http client, but expose the same interface to the user.
  // def client(): CitizenService = ???
}
