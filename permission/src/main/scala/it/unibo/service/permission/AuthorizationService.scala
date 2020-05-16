package it.unibo.service.permission

import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService

trait AuthorizationService {
  def authorizeRead(who : String, citizen: String, category : DataCategory) : FutureService[DataCategory]
  def authorizeWrite(who : String, citizen: String, category: DataCategory) : FutureService[DataCategory]
  def authorizedReadCategories(who : String, citizen: String) : FutureService[Seq[DataCategory]]
  def authorizedWriteCategories(who : String, citizen: String) : FutureService[Seq[DataCategory]]
}
