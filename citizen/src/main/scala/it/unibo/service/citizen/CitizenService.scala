package it.unibo.service.citizen

import it.unibo.core.data._
import it.unibo.core.dt.History.History
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.{AuthService, SystemUser}
import it.unibo.service.permission.AuthorizationService

/**
 * Abstraction of Citizen Service expressed using main domain concept.
 * key idea: allow to expose the same service through different web technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: String, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]]
  def readState(who: String, citizenId: String): FutureService[Seq[Data]]
  def readHistory(who: String, citizenId: String, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]
  def readHistoryData(who: String, citizenId: String, dataIdentifier: String): FutureService[Data]
  // TODO: define better using rx scala
  def observeState(who: String, citizenId: String, callback : Data => Unit): FutureService[Channel]

  trait Channel {
    def updateState(data: Seq[Data]): FutureService[Seq[Data]]
    def readState(): FutureService[Seq[Data]]
    def close() : Unit
  }

  protected trait SourceChannel extends Channel {
    def emit(data : Seq[Data]) : Unit
  }
}

object CitizenService {

  /**
   * Create the default BackendService
   * @param authorizationService Service that provide authorization check
   * @param dataStorage Storage where save the citizen data
   * @return A Citizen Service backend instance
   */
  def apply(authorizationService: AuthorizationService,
            authenticationService : AuthService,
            dataStorage: Storage[Data, String]): CitizenService = new BackendCitizenService(authorizationService, authenticationService, dataStorage)

  // the same interface could be used for create the client counterpart. Useful for test the backend.
  // e.g. a client could use vertx http client, but expose the same interface to the user.
  // def client(): CitizenService = ???
}
