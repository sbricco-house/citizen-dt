package it.unibo.service.permission

import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService
import it.unibo.core.authentication.SystemUser

trait AuthorizationService {
  def authorizeRead(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory]
  def authorizeWrite(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory]
  def authorizedReadCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]]
  def authorizedWriteCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]]
}