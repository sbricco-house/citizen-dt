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
  def citizenIdentifier : String
  def updateState(who: TokenIdentifier,data: Seq[Data]): FutureService[Seq[String]]
  def readState(who: TokenIdentifier): FutureService[Seq[Data]]
  def readStateByCategory(who: TokenIdentifier, category : DataCategory): FutureService[Seq[Data]]
  def readHistory(who: TokenIdentifier, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]
  def readHistoryData(who: TokenIdentifier, dataIdentifier: String): FutureService[Data]
  def createPhysicalLink(who: TokenIdentifier): FutureService[PhysicalLink]

  trait PhysicalLink {
    def updateState(data: Seq[Data]): FutureService[Seq[String]]
    def updateDataStream() : Observable[Data]
    def close() : Unit
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
            citizenId : String,
            dataStorage: Storage[Data, String]): CitizenService = {
    implicit val execution = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    new BackendCitizenService(authenticationService, authorizationService, citizenId, dataStorage)
  }

  def fromVertx(authenticationService : AuthenticationService,
            authorizationService: AuthorizationService,
            citizenId : String,
            dataStorage: Storage[Data, String],
            vertx: Vertx): CitizenService = {
    implicit val execution = VertxExecutionContext(vertx.getOrCreateContext())
    new BackendCitizenService(authenticationService, authorizationService, citizenId, dataStorage)
  }
}
