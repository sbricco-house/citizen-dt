package it.unibo.service.citizen.authorization

import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService

trait AuthorizationService {
  def authorizeRead(authenticated : String, citizen: String, category : DataCategory) : FutureService[DataCategory]
  def authorizeWrite(authenticated : String, citizen: String, category: DataCategory) : FutureService[DataCategory]
  def authorizedReadCategories(authenticated : String, citizen: String) : FutureService[Seq[DataCategory]]
  def authorizedWriteCategories(authenticated : String, citizen: String) : FutureService[Seq[DataCategory]]
}
