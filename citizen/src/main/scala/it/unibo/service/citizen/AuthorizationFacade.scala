package it.unibo.service.citizen

import it.unibo.core.data.DataCategory

import scala.concurrent.Future

trait AuthorizationFacade {
  def authorizeRead(who : String, category : DataCategory) : Future[Unit]
  def authorizeWrite(who : String, category: DataCategory) : Future[Unit]
  def authorizedReadCategory(who : String) : Future[Seq[DataCategory]]
  def authorizedWriteCategory(who : String) : Future[Seq[DataCategory]]
}
