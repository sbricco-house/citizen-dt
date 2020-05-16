package it.unibo.service.citizen

import it.unibo.core.data._
import it.unibo.core.dt.History.History
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.SystemUser
import it.unibo.service.permission.AuthorizationService

/**
 * Abstraction of Citizen Service expressed using main domain concept.
 * key idea: allow to expose the same service through different web technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]]
  def readState(who: SystemUser, citizenId: String): FutureService[Seq[Data]]
  def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]
  def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): FutureService[Option[Data]]

  // TODO: define better using rx scala
  // def observeState(who: SystemUser, citizenId: String): Observable[Seq[Data]]
}

object CitizenService {

  /**
   * Create the default BackendService
   * @param authorizationService Service that provide authorization check
   * @param dataStorage Storage where save the citizen data
   * @return A Citizen Service backend instance
   */
  def apply(authorizationService: AuthorizationService,
            dataStorage: Storage[Data, String]): CitizenService = new BackendCitizenService(authorizationService, dataStorage)

  // the same interface could be used for create the client counterpart. Useful for test the backend.
  // e.g. a client could use vertx http client, but expose the same interface to the user.
  // def client(): CitizenService = ???
}
