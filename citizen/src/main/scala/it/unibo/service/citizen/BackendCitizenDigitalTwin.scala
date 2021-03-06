package it.unibo.service.citizen

import it.unibo.core.authentication.{SystemUser, TokenIdentifier}
import it.unibo.core.data.{Data, DataCategory, DataCategoryOps, LeafCategory, Storage}
import it.unibo.core.dt.History.History
import it.unibo.core.dt.State
import it.unibo.core.microservice.FutureService
import it.unibo.core.utils.ServiceError.{MissingParameter, MissingResource, Unauthorized}
import it.unibo.service.authentication.AuthenticationService
import it.unibo.service.permission.AuthorizationService
import monix.reactive.{MulticastStrategy, Observable}
import monix.reactive.subjects.{ConcurrentSubject, PublishSubject}

import scala.concurrent.ExecutionContext
import scala.util.Success

/**
 * Implementation of backend Citizen Digital twin
 *
 * @param authenticationService Service for authenticate the user of system
 * @param authorizationService Service for authorize the system user to do some action
 * @param dataStorage Storage where save the data of citizen
 * @param state Current state of citizen, the default is empty.
 */
class BackendCitizenDigitalTwin(authenticationService : AuthenticationService,
                                authorizationService: AuthorizationService,
                                override val citizenIdentifier : String,
                                dataStorage : HistoryStorage,
                                private var state: State = State.empty,
                            )(implicit val executionContext: ExecutionContext) extends CitizenDigitalTwin {
  self =>

  //load the state of citizen from data storage if the state is empty
  dataStorage.extractState() match {
    case Success(state) if(state != State.empty) => this.state = state
    case _ =>
  }

  private val observableState = PublishSubject[Data]()
  private var channels : Map[PhysicalLink, TokenIdentifier] = Map.empty

  override def readState(who: TokenIdentifier): FutureService[Seq[Data]] = {
    authenticationService.verifyToken(who)
      .flatMap(_ => authorizationService.authorizedReadCategories(who, citizen = citizenIdentifier))
      .map(categories => state.get(categories))
  }

  override def readStateByCategory(who: TokenIdentifier, category: DataCategory): FutureService[Seq[Data]] = {
    authenticationService.verifyToken(who)
      .flatMap(_ => authorizationService.authorizeRead(who, citizen = citizenIdentifier, category))
      .map(categories => state.get(categories))
  }

  override def updateState(who: TokenIdentifier, data: Seq[Data]): FutureService[Seq[String]] = {
    authenticationService.verifyToken(who).flatMap(user => internalUpdateState(who, data))
  }

  override def readHistory(who: TokenIdentifier, dataCategory: DataCategory, maxSize: Int): FutureService[History] = {
    authenticationService.verifyToken(who)
      .flatMap(user => authorizationService.authorizeRead(who, citizenIdentifier, dataCategory))
      .map(category => findHistoryInStorage(category, maxSize))
  }

  override def readHistoryData(who: TokenIdentifier, dataIdentifier: String): FutureService[Data] = {
    authenticationService.verifyToken(who)
      .map(user => (user, findDataInStorage(dataIdentifier)))
      .flatMap {
        case (_, pendingData) => pendingData.flatMap {
          data => {
            authorizationService.authorizeRead(who, citizenIdentifier, data.category).map(_ => data)
          }
        }
      }
  }

  override def createPhysicalLink(who: TokenIdentifier): FutureService[PhysicalLink] = {
    authenticationService.verifyToken(who)
      .flatMap(user => {
        authorizationService.authorizedReadCategories(who, self.citizenIdentifier)
          .map(categories => new SourceImpl(who, categories))
      })
  }

  override def observeState(who: TokenIdentifier, dataCategory: DataCategory): FutureService[Observable[Data]] = {
    authenticationService.verifyToken(who)
      .flatMap(user => authorizationService.authorizeRead(who, citizenIdentifier, dataCategory))
      .map(DataCategoryOps.allChild)
      .map(categories => observableState.filter(data => categories.contains(data.category)))
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

  protected def internalUpdateState(who : TokenIdentifier, data : Seq[Data]) : FutureService[Seq[String]] ={
    data.map(_.category) match {
      case Nil => FutureService.fail(MissingParameter(s"Invalid set of data"))
      case categoriesToUpdate => authorizationService.authorizedWriteCategories(who, citizen = citizenIdentifier)
        .flatMap {
          case categories if checkPermission(categoriesToUpdate, categories) == categoriesToUpdate => FutureService.response(save(data))
          case _ => FutureService.fail(Unauthorized(s"Not enough permission to do that"))
        }
    }
  }

  private def save(dataSequence: Seq[Data]): Seq[String] = {
    val savedData = dataSequence.map(data => dataStorage.store(data.identifier, data))
        .filter(result => result.isSuccess).map(_.get)
    savedData.foreach(data => {
      state = state.update(data)
      this.observableState.onNext(data)
    })
    savedData.map(_.identifier)
  }
  // TODO: move from here
  private def checkPermission(categoriesToUpdate: Seq[LeafCategory], authorizedCategories: Seq[DataCategory]): Seq[LeafCategory] = {
    categoriesToUpdate.filter {
      leaf => authorizedCategories.exists(DataCategoryOps.allChild(_).exists(leaf.name == _.name))
    }
  }

  private class SourceImpl(user : TokenIdentifier, categories : Seq[DataCategory]) extends PhysicalLink {
    self.channels += this -> user
    private val flattenCategory = categories.flatMap(DataCategoryOps.allChild).toSet
    private val publishChannel = self.observableState.filter(data => flattenCategory.contains(data.category))
    //TODO this method may avoid to call update state, it has categories already
    override def updateState(data: Seq[Data]): FutureService[Seq[String]] = self.internalUpdateState(user, data)

    override def close(): Unit = self.channels -= this

    override def updateDataStream(): Observable[Data] = publishChannel
  }
}