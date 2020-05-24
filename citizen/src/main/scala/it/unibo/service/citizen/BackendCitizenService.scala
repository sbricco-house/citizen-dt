package it.unibo.service.citizen

import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.{Data, DataCategory, DataCategoryOps, LeafCategory, Storage}
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.authentication.{AuthenticationService, TokenIdentifier}
import it.unibo.service.permission.AuthorizationService
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

import scala.concurrent.ExecutionContext
import scala.util.Success

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
                            override val citizenIdentifier : String,
                            dataStorage : Storage[Data, String],
                            private var state: State = State.empty,
                            )(implicit val executionContext: ExecutionContext) extends CitizenService {
  self =>

  private var channels : Map[SourcePhysicalLink, SystemUser] = Map.empty

  override def readState(who: TokenIdentifier): FutureService[Seq[Data]] = {
    authenticationService.verifyToken(who).flatMap(readState)
  }

  override def updateState(who: TokenIdentifier, data: Seq[Data]): FutureService[Seq[Data]] = {
    authenticationService.verifyToken(who).flatMap(user => updateState(user, data))
  }

  override def readHistory(who: TokenIdentifier, dataCategory: DataCategory, maxSize: Int): FutureService[History] = {
    authenticationService.verifyToken(who)
      .flatMap(user => authorizationService.authorizeRead(user, citizenIdentifier, dataCategory))
      .map(category => findHistoryInStorage(category, maxSize))
  }

  override def readHistoryData(who: TokenIdentifier, dataIdentifier: String): FutureService[Data] = {
    authenticationService.verifyToken(who)
        .map(user => (user, findDataInStorage(dataIdentifier)))
        .flatMap {
          case (user, pendingData) => pendingData.flatMap {
            data => {
              authorizationService.authorizeRead(user, citizenIdentifier, data.category).map(_ => data)
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

  protected def updateState(who : SystemUser, data : Seq[Data]) : FutureService[Seq[Data]] ={
    data.map(_.category) match {
      case Nil => FutureService.fail(MissingParameter(s"Invalid set of data"))
      case categoriesToUpdate => authorizationService.authorizedWriteCategories(who, citizen = citizenIdentifier)
        .flatMap {
          case categories if checkPermission(categoriesToUpdate, categories) == categoriesToUpdate => FutureService.response(save(data))
          case _ => FutureService.fail(Unauthorized(s"Not enough permission to do that"))
        }
    }
  }

  // TODO: move from here
  private def checkPermission(categoriesToUpdate: Seq[LeafCategory], authorizedCategories: Seq[DataCategory]): Seq[LeafCategory] = {
    categoriesToUpdate.filter {
      leaf => authorizedCategories.exists(DataCategoryOps.allChild(_).contains(leaf))
    }
  }

  protected def readState(who : SystemUser) : FutureService[Seq[Data]] = {
    authorizationService.authorizedReadCategories(who, citizen = citizenIdentifier)
      .map(categories => state.get(categories))
  }

  private class SourceImpl(user : SystemUser, categories : Seq[DataCategory]) extends SourcePhysicalLink {
    self.channels += this -> user
    val publishChannel = PublishSubject[Data]()
    //todo handle on next
    override def emit(data: Seq[Data]): Unit = data foreach publishChannel.onNext

    //TODO this method may avoid to call update state, it has categories already
    override def updateState(data: Seq[Data]): FutureService[Seq[Data]] = self.updateState(user, data)

    override def close(): Unit = self.channels -= this

    override def updateDataStream(): Observable[Data] = publishChannel
  }

  override def observeState(who: TokenIdentifier): FutureService[PhysicalLink] = {
    authenticationService.verifyToken(who)
      .flatMap(user => {
        authorizationService.authorizedReadCategories(user, self.citizenIdentifier)
          .map(categories => new SourceImpl(user, categories))
      })
  }
}