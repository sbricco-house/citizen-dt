package it.unibo.service.permission

import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}

trait AuthorizationService {
  def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory]
  def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory]
  def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]]
  def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]]
}