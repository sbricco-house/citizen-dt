package it.unibo.client.demo.controller

import java.util.concurrent.Executors

import it.unibo.client.demo.AuthUserProvider
import it.unibo.core.data.{Data, DataCategory, LeafCategory}
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.service.citizen.client.CitizenClient
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

import scala.concurrent.ExecutionContext

/**
 * This controller provide to the gui a unique CDT State point of view through the observable.
 * It use a citizen client to make request and other things
 * @param authProvider
 * @param client
 */

// TODO: maintain a list of observing categories, allowing to close the observation
class CDTController(dataSimulator: DataSimulator, authProvider: AuthUserProvider, client: CitizenClient) {

  private implicit val context = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
  private val monixContext = Scheduler(context)

  private var physicalLink: Option[CDTController.this.client.PhysicalLink] = None
  private val _state: PublishSubject[Seq[Data]] = PublishSubject()
  private var internalState: Map[DataCategory, Data] = Map()

  val state: Observable[Seq[Data]] = _state

  dataSimulator.simulatedData.foreach {
    data => updateState(Seq(data))
  }(monixContext)

  def canObserveCitizen: Boolean = authProvider.currentUser().role != "citizen"

  def fetchState(): FutureService[_] = {
    client.readState(authProvider.currentToken()).future.transform({
      case Response(content) => updateCurrentState(content); Response()
      case f: Fail[_] => f
    }, identity).toFutureService
  }

  def updateState(data: Seq[Data]): FutureService[Seq[String]] = {
    if(physicalLink.nonEmpty) {
      physicalLink.get.updateState(data)
    } else {
      client.updateState(authProvider.currentToken(), data)
    }
  }

  def createLink(): FutureService[_] = {
    client.createPhysicalLink(authProvider.currentToken()).future.transform({
      case Response(content) => bindPhysicalLink(content); Response();
      case f: Fail[_] => f
    }, identity).toFutureService
  }

  def observe(categoryName: String): FutureService[_] = {
    client.observeState(authProvider.currentToken(), LeafCategory(categoryName, -1)).future.transform({
      case Response(content) => bindToUpdateStateObservable(content); Response()
      case f: Fail[_] => f
    }, identity).toFutureService
  }

  private def updateCurrentState(data: Seq[Data]): Unit = {
    internalState ++= data.map(d => (d.category, d)).toMap
    _state.onNext(internalState.values.toSeq)
  }

  private def bindPhysicalLink(link: CDTController.this.client.PhysicalLink): Unit = {
    physicalLink = Some(link)
    bindToUpdateStateObservable(link.updateDataStream())
  }

  private def bindToUpdateStateObservable(linkObservable: Observable[Data]): Unit = {
    linkObservable.observeOn(monixContext).foreach {
      case data: Data => updateCurrentState(Seq(data))
      case _ =>
    }(monixContext)
  }
}
