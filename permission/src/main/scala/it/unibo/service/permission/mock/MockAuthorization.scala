package it.unibo.service.permission.mock

import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.data.{DataCategory, DataCategoryOps, GroupCategory, LeafCategory}
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceRuntime}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.permission.AuthorizationService

object MockAuthorization {
  def apply(authorization: Map[(String, String), Seq[DataCategory]], idMapping : String => String = a => a): MockAuthorization = new MockAuthorization(authorization, idMapping)

  def acceptAll(registry : DataParserRegistry[_]) : AuthorizationService = new AuthorizationService {
    override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = FutureService.response(category)
    override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = FutureService.response(category)
    override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = FutureService.response(registry.supportedCategories)
    override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = FutureService.response(registry.supportedCategories)

    override def toString: String = s"AuthorizationAcceptAll ${registry.supportedCategories}"
  }
}

class MockAuthorization(private val authorization: Map[(String, String), Seq[DataCategory]], idMapping : String => String) extends AuthorizationService {
  override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(who.token, citizen, category)

  override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(who.token, citizen, category)

  override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(who.token, citizen)

  override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(who.token, citizen)

  private def checkRW(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val response = authorization.get((idMapping(authenticated), citizen))
      .flatMap(categories => checkPermission(categories, category))
      .map(Response(_))
      .getOrElse(Fail(Unauthorized(s"Cannot access to ${category.name} data of citizen $citizen")))

    FutureService(response)
  }

  private def authorizedCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] = {
    authorization.get((idMapping(authenticated), citizen)) match {
      case Some(value) => FutureService.response(value)
      case None => FutureService.fail(Unauthorized(s"User $authenticated cannot access to $citizen"))
    }
  }

  private def checkPermission(authorizeCategories: Seq[DataCategory], requestCategory: DataCategory): Option[DataCategory] = requestCategory match {
    case GroupCategory(name, _) => authorizeCategories.find(_.name == name)
    case LeafCategory(name, _) => DataCategoryOps.allChild(requestCategory).find(leaf => leaf.name == name)
  }
}
