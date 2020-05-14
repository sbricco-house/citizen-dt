package it.unibo.service.citizen

import java.util.UUID

import it.unibo.core.data.{Data, DataCategory, Storage}
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.service.authentication.SystemUser
import it.unibo.service.citizen.authorization.AuthorizationService
import it.unibo.service.citizen.utils._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
 * The interface for access to services provided by Citizen Service, expressed as main domain concept.
 * It allow to expose the same service through different technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): Future[Seq[Data]]
  def readState(who: SystemUser, citizenId: String): Future[Seq[Data]]
  def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int = 1): Future[History]
  def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): Future[Option[Data]]
}

object CitizenService {
  def apply(authorizationFacade: AuthorizationService,
            dataStorage: Storage[Data, String]): CitizenService = new CitizenServiceLogic(authorizationFacade, dataStorage)

  // the same interface could be used for create the client counterpart. Useful for test the backend.
  // e.g. a client could use vertx http client, but expose the same interface to the user.
  def client(): CitizenService = ???
}

/**
 * Implementation of backend CitizenService
 * @param authorizationService Service for authorize the system user to do some action
 * @param dataStorage Storage where save the data of citizen
 * @param state Current state of citizen, the default is empty.
 */
class CitizenServiceLogic(authorizationService: AuthorizationService,
                          dataStorage : Storage[Data, String],
                          private var state: State = State.empty) extends CitizenService {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def readState(who: SystemUser, citizenId: String): Future[Seq[Data]] = {
    authorizationService.authorizedReadCategories(who.identifier, citizenId)
      .map(authorizedCategories => state.get(authorizedCategories))
  }

  override def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): Future[Seq[Data]] = {
    val categoriesToUpdate = data.map(_.category)
    authorizationService.authorizedWriteCategories(who.identifier, citizenId).map {
      case categories if categories == categoriesToUpdate => save(data)
    }
  }

  override def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int): Future[History] = {
    authorizationService.authorizeRead(who.identifier, citizenId, dataCategory)
        .flatMap(category => Future.fromTry(dataStorage.findMany(_.category == category, Some(maxSize))))
  }

  override def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): Future[Option[Data]] = {
    Future.fromTry(dataStorage.get(dataIdentifier))
      .toFutureOption
      .flatMap(data => authorizationService.authorizeRead(who.identifier, citizenId, data.category).map(_ => Some(data)).toFutureOption)
      .future
  }

  private def save(dataSequence: Seq[Data]): Seq[Data] = {
    dataSequence.foreach { data =>
      dataStorage.store(UUID.randomUUID().toString, data)
      state = state.update(data)
    }
    dataSequence
  }
}
