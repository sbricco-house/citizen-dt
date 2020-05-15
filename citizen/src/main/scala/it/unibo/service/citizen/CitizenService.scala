package it.unibo.service.citizen

import java.util.UUID

import it.unibo.core.data._
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.authentication.SystemUser
import it.unibo.service.citizen.authorization.AuthorizationService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/**
 * Abstraction of Citizen Service expressed using main domain concept.
 * key idea: allow to expose the same service through different web technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]]
  def readState(who: SystemUser, citizenId: String): FutureService[Seq[Data]]
  def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]
  def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): FutureService[Option[Data]]
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
  def client(): CitizenService = ???
}
