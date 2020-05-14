package it.unibo.service.citizen.authorization

import it.unibo.core.data.DataCategory

import scala.concurrent.Future

trait AuthorizationService {
  def authorizeRead(authenticated : String, citizen: String, category : DataCategory) : Future[DataCategory]
  def authorizeWrite(authenticated : String, citizen: String, category: DataCategory) : Future[DataCategory]
  def authorizedReadCategories(authenticated : String, citizen: String) : Future[Seq[DataCategory]]
  def authorizedWriteCategories(authenticated : String, citizen: String) : Future[Seq[DataCategory]]
}
