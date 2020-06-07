package it.unibo.service.citizen

import java.util.concurrent.Executors

import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.data._
import it.unibo.core.dt.History.History
import it.unibo.core.microservice.FutureService
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.permission.AuthorizationService
import monix.reactive.Observable

import scala.concurrent.ExecutionContext

/**
 * Abstraction of Citizen Digital Twin (CDT) expressed using main domain concept.
 * Key idea: allow to expose the same service through different web technology/interface, e.g. websocket, rest api, ecc...
 * It wraps a state (i.e. current snapshot of citizen relevant information) and historical data (i.e. old information linked to a citizen)
 * A CDT could be observed by stakeholder that have permission to watch citizen updates.
 * each real citizen can create a physical link (i.e. a two-way persistent connection) in which it could update his digital state and being notified from
 * state changes.
 */
trait CitizenDigitalTwin {
  /**
   * @return an identifier linked uniquely to a real citizen
   */
  def citizenIdentifier : String

  /**
   * update the state to the citizen digital twin
   * @param who The one who would change the citizen state
   * @param data A series of new information linked to the citizen
   * @return Success(data) if @who could change citizen, Fail(error) if there are some error (e.g authorization issue) during update
   */
  def updateState(who: TokenIdentifier,data: Seq[Data]): FutureService[Seq[String]]

  /**
   * get current citizen state snapshot
   * @param who The one who would get the citizen state
   * @return Success(data) if @who could read citizen state, Fail(error) if there are some error (e.g authorization issue) during reading
   */
  def readState(who: TokenIdentifier): FutureService[Seq[Data]]

  /**
   * get current citizen state snapshot filtered by a data category (e.g. Position, medical,...)
   * @param who The one who would get the citizen state
   * @param category The category used to filter the current citizen state
   * @return Success(data) if @who could read citizen state, Fail(error) if there are some error (e.g authorization issue) during reading
   */
  def readStateByCategory(who: TokenIdentifier, category : DataCategory): FutureService[Seq[Data]]

  /**
   * get citizen historical data filtered by a data category (e.g. Position, medical,...)
   * @param who The one who would get the citizen historical data
   * @param dataCategory The category used to filter the citizen history
   * @param maxSize max elements taken by historical data
   * @return Success(data) if @who could read citizen history, Fail(error) if there are some error (e.g authorization issue) during reading
   */
  def readHistory(who: TokenIdentifier, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]

  /**
   * get citizen historical data filtered by the dataIdentifier
   * @param who The one who would get the specific historical data
   * @param dataIdentifier
   * @return Success(data) if @who could read citizen history, Fail(error) if there are some error (e.g authorization issue) during reading
   */
  def readHistoryData(who: TokenIdentifier, dataIdentifier: String): FutureService[Data]

  /**
   * create a link between real and digital citizen.
   * @param who The citizen who would get the persistent channel
   * @return Success(Link) if @who is citizen, Fail(error) otherwise
   */
  def createPhysicalLink(who: TokenIdentifier): FutureService[PhysicalLink]

  /**
   * observe a state evolution of citizen filtered by data category
   * @param who The one who would observe citizen state evolution
   * @param dataCategory The observed data category (e.g. MedicalData,...)
   * @return Sucess(observable) if @who could observe the state, Fail(error) otherwisie
   */
  def observeState(who: TokenIdentifier, dataCategory : DataCategory): FutureService[Observable[Data]]

  /**
   * a persistent channel between real and digital entity. Its goals is maintain the digital state
   * consistent with the physical one and viceversa.
   */
  trait PhysicalLink {
    /**
     * update the citizen state with new information
     * @param data the new data to add into the citizen state
     * @return Success(data) if the data are valid, Fail otherwise
     */
    def updateState(data: Seq[Data]): FutureService[Seq[String]]

    /**
     * an observable source in which each state alternation is sent.
     * This follow the Rx observable pattern (i.e. a concurrent extension of GoF pattern observer)
     * @return the observable link to the state alternation
     */
    def updateDataStream() : Observable[Data]

    /**
     * close the link and release all resources.
     */
    def close() : Unit
  }
}

object CitizenDigitalTwin {

  /**
   * Create the default CDT implementation
   * @param authenticationService Service that provide authentication
   * @param authorizationService Service that provide authorization check
   * @param citizenId the identifier associated to the real citizen
   * @param dataStorage Storage where save the citizen data
   * @return A Citizen DT instance
   */
  def apply(authenticationService : AuthenticationService,
            authorizationService: AuthorizationService,
            citizenId : String,
            dataStorage: HistoryStorage): CitizenDigitalTwin = {
    implicit val execution = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
    new BackendCitizenDigitalTwin(authenticationService, authorizationService, citizenId, dataStorage)
  }

  /**
   * create CDT that use vertx framework
   * @param authenticationService Service that provide authentication
   * @param authorizationService Service that provide authorization check
   * @param citizenId the identifier associated to the real citizen
   * @param dataStorage Storage where save the citizen data
   * @param vertx the vertx instance used by citizen to handle requests.
   * @return A Citizen DT instance
   */
  def fromVertx(authenticationService : AuthenticationService,
            authorizationService: AuthorizationService,
            citizenId : String,
            dataStorage: HistoryStorage,
            vertx: Vertx): CitizenDigitalTwin = {

    implicit val execution = VertxExecutionContext(vertx.getOrCreateContext())
    new BackendCitizenDigitalTwin(authenticationService, authorizationService, citizenId, dataStorage)
  }
}
