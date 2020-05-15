package it.unibo.service.citizen

import java.util.UUID

import it.unibo.core.data.{Data, DataCategory, GroupCategory, LeafCategory, Storage}
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.authentication.SystemUser
import it.unibo.service.citizen.authorization.AuthorizationService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.Success

/**
 * Implementation of backend CitizenService
 *
 * @param authorizationService Service for authorize the system user to do some action
 * @param dataStorage Storage where save the data of citizen
 * @param state Current state of citizen, the default is empty.
 */
class BackendCitizenService(authorizationService: AuthorizationService,
                            dataStorage : Storage[Data, String],
                            private var state: State = State.empty) extends CitizenService {

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def readState(who: SystemUser, citizenId: String): FutureService[Seq[Data]] = {
    authorizationService.authorizedReadCategories(who.identifier, citizenId)
      .map(categories => state.get(categories))
  }

  override def updateState(who: SystemUser, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]] = {
    data.map(_.category) match {
      case Nil => FutureService.fail(MissingParameter(s"Invalid set of data"))
      case categoriesToUpdate => authorizationService.authorizedWriteCategories(who.identifier, citizenId)
        .flatMap {
          case categories if _flatGroupCategories(categories) == categoriesToUpdate => FutureService.response(save(data))
          case _ => FutureService.fail(Unauthorized())
        }
    }
  }
  def _flatGroupCategories(categories: Seq[DataCategory]): Seq[DataCategory] = {
    categories.flatMap {
      case GroupCategory(_, dataCategory) => dataCategory
      case cat @ LeafCategory(_, _) => Seq(cat)
    }.distinct
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
