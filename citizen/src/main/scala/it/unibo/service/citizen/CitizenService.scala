package it.unibo.service.citizen

import java.util.UUID

import it.unibo.core.data._
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.utils.ServiceError.{MissingResource, Unauthorized}
import it.unibo.service.authentication.SystemUser
import it.unibo.service.citizen.authorization.AuthorizationService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/**
 * The interface for access to services provided by Citizen Service, expressed as main domain concept.
 * It allow to expose the same service through different technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]]
  def readState(who: SystemUser, citizenId: String): FutureService[Seq[Data]]
  def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int = 1): FutureService[History]
  def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): FutureService[Option[Data]]
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

  override def readState(who: SystemUser, citizenId: String): FutureService[Seq[Data]] = {
    authorizationService.authorizedReadCategories(who.identifier, citizenId)
      .map(categories => state.get(categories))
  }

  override def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]] = {
    val categoriesToUpdate = data.map(_.category)
    authorizationService.authorizedWriteCategories(who.identifier, citizenId)
      .flatMap {
          case categories if categories == categoriesToUpdate => FutureService.response(save(data))
          case _ => FutureService.fail(Unauthorized())
        }
  }

  override def readHistory(who: SystemUser, citizenId: String, dataCategory: DataCategory, maxSize: Int): FutureService[History] = {
    authorizationService.authorizeRead(who.identifier, citizenId, dataCategory)
        .map(category => findHistoryInStorage(category, maxSize))
  }

  override def readHistoryData(who: SystemUser, citizenId: String, dataIdentifier: String): FutureService[Option[Data]] = {
    findDataInStorage(dataIdentifier)
      .flatMap(data => authorizationService.authorizeRead(who.identifier, citizenId, data.category).map(_ => Option(data)))
  }

  // TODO: TRY TO LAUNCH EXCEPTION INSIDE FIND MANY
  private def findHistoryInStorage(category: DataCategory, maxSize: Int): Seq[Data] = {
    dataStorage.findMany(_.category == category, Some(maxSize)) match {
      case Success(value) => value
      case _ => Seq()
    }
  }

  private def findDataInStorage(dataIdentifier: String): FutureService[Data] = {
    dataStorage.get(dataIdentifier) match {
      case Success(Some(value)) => FutureService.response(value)
      case Success(None) => FutureService.fail(MissingResource(s"Data $dataIdentifier not found"))
    }
  }

  private def save(dataSequence: Seq[Data]): Seq[Data] = {
    dataSequence.foreach { data =>
      dataStorage.store(UUID.randomUUID().toString, data)
      state = state.update(data)
    }
    dataSequence
  }
}
