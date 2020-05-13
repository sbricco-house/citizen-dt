package it.unibo.service.citizen

import it.unibo.core.data.{Data, Storage}
import it.unibo.core.dt.State
import it.unibo.service.authentication.SystemUser
import it.unibo.service.citizen.authorization.AuthorizationService

import scala.concurrent.{ExecutionContext, Future}

/**
 * The interface for access to service provided by Citizen Service, expressed as main domain concept.
 * It allow to expose the same service through different technology/interface, e.g. websocket, rest api, ecc...
 */
trait CitizenService {
  def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): Future[Seq[Data]]
  def readState(who: SystemUser, citizenId: String): Future[Seq[Data]]
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

  implicit val executionContext = ExecutionContext.global

  override def readState(who: SystemUser, citizenId: String): Future[Seq[Data]] = {
    val authorizedCategories = authorizationService.authorizedReadCategories(who.identifier, citizenId)
    authorizedCategories.map {
      case List(categories) => state.get(categories)
    }
  }

  override def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): Future[Seq[Data]] = {
    val authorizedCategories = authorizationService.authorizedWriteCategories(who.identifier, citizenId)
    val categoriesToUpdate = data.map(_.category)
    authorizedCategories.map {
      case List(categories) if categories == categoriesToUpdate => save(data)
      case _ => Seq()
    }
  }

  private def save(dataSequence: Seq[Data]): Seq[Data] = {
    dataSequence.foreach { data =>
      dataStorage.store(data.identifier, data)
      state = state.update(data)
    }
    dataSequence
  }
}
