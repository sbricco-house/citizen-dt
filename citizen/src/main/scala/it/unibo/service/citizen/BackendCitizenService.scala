package it.unibo.service.citizen

import java.util.UUID

import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.{Data, DataCategory, DataCategoryOps, LeafCategory, Storage}
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.authentication.{AuthenticationService, TokenIdentifier}
import it.unibo.service.permission.AuthorizationService

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

/**
 * Implementation of backend CitizenService
 *
 * @param authenticationService Service for authenticate the user of system
 * @param authorizationService Service for authorize the system user to do some action
 * @param dataStorage Storage where save the data of citizen
 * @param state Current state of citizen, the default is empty.
 */
class BackendCitizenService(authenticationService : AuthenticationService,
                            authorizationService: AuthorizationService,
                            dataStorage : Storage[Data, String],
                            private var state: State = State.empty) extends CitizenService {
  self =>

  private var channels : Map[SourceChannel, SystemUser] = Map.empty

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  override def readState(who: TokenIdentifier, citizenId: String): FutureService[Seq[Data]] = {
    authenticationService.verifyToken(who).flatMap(user => readState(user, citizenId))
  }

  override def updateState(who: TokenIdentifier, citizenId: String, data: Seq[Data]): FutureService[Seq[Data]] = {
    authenticationService.verifyToken(who).flatMap(user => updateState(user, citizenId, data))
  }

  override def readHistory(who: TokenIdentifier, citizenId: String, dataCategory: DataCategory, maxSize: Int): FutureService[History] = {
    authenticationService.verifyToken(who)
      .flatMap(user => authorizationService.authorizeRead(user, citizenId, dataCategory))
      .map(category => findHistoryInStorage(category, maxSize))
  }

  override def readHistoryData(who: TokenIdentifier, citizenId: String, dataIdentifier: String): FutureService[Data] = {
    authenticationService.verifyToken(who)
        .map(user => (user, findDataInStorage(dataIdentifier)))
        .flatMap {
          case (user, pendingData) => pendingData.flatMap {
            data => {
              authorizationService.authorizeRead(user, citizenId, data.category).map(_ => data)
            }
          }
        }
  }

  private def findHistoryInStorage(category: DataCategory, maxSize: Int): Seq[Data] = {
    dataStorage.findMany(data => DataCategoryOps.contains(category, data.category).isDefined, maxSize) match {
      case Success(value) => value
      case _ => Seq()
    }
  }

  private def findDataInStorage(dataIdentifier: String): FutureService[Data] = {
    dataStorage.get(dataIdentifier) match {
      case Success(Some(value)) => FutureService.response(value)
      case Success(None) => FutureService.fail(MissingResource(s"Data $dataIdentifier not found"))
      case _ => FutureService.fail()
    }
  }

  private def save(dataSequence: Seq[Data]): Seq[Data] = {
    //TODO FIX
    this.channels.keys.foreach(_.emit(dataSequence))
    val savedData = dataSequence.map(data => dataStorage.store(data.identifier, data))
        .filter(result => result.isSuccess).map(_.get)
    savedData.foreach(data => state = state.update(data))
    savedData
  }

  protected def updateState(who : SystemUser, citizenId : String, data : Seq[Data]) : FutureService[Seq[Data]] ={
    data.map(_.category) match {
      case Nil => FutureService.fail(MissingParameter(s"Invalid set of data"))
      case categoriesToUpdate => authorizationService.authorizedWriteCategories(who, citizen = citizenId)
        .flatMap {
          case categories if checkPermission(categoriesToUpdate, categories) == categoriesToUpdate => FutureService.response(save(data))
          case _ => FutureService.fail(Unauthorized())
        }
    }
  }

  // TODO: move from here
  private def checkPermission(categoriesToUpdate: Seq[LeafCategory], authorizedCategories: Seq[DataCategory]): Seq[LeafCategory] = {
    categoriesToUpdate.filter {
      leaf => authorizedCategories.exists(DataCategoryOps.allChild(_).contains(leaf))
    }
  }

  protected def readState(who : SystemUser, citizenId : String) : FutureService[Seq[Data]] = {
    authorizationService.authorizedReadCategories(who, citizen = citizenId)
      .map(categories => state.get(categories))
  }

  private class SourceImpl(callback : Data => Unit, user : SystemUser, citizen : String, categories : Seq[DataCategory]) extends SourceChannel {
    self.channels += this -> user

    override def emit(data: Seq[Data]): Unit = data foreach callback

    //TODO this method may avoid to call update state, it has categories already
    override def updateState(data: Seq[Data]): FutureService[Seq[Data]] = self.updateState(user, citizen, data)

    override def close(): Unit = self.channels -= this
  }

  override def observeState(who: TokenIdentifier, citizenId: String, callback: Data => Unit): FutureService[Channel] = {
    authenticationService.verifyToken(who)
      .flatMap(user => {
        authorizationService.authorizedReadCategories(user, citizenId)
          .map(categories => new SourceImpl(callback, user, citizenId, categories))
      })
  }
}
