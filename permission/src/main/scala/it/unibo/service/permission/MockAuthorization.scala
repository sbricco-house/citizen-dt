package it.unibo.service.permission

import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.{DataCategory, DataCategoryOps, GroupCategory, LeafCategory}
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized

object MockAuthorization {
  def apply(authorization: Map[(String, String), Seq[DataCategory]]): MockAuthorization = new MockAuthorization(authorization)

  def acceptAll(registry : DataParserRegistry[_]) : AuthorizationService = new AuthorizationService {
    override def authorizeRead(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = FutureService.response(category)
    override def authorizeWrite(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = FutureService.response(category)
    override def authorizedReadCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = FutureService.response(registry.supportedCategories)
    override def authorizedWriteCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = FutureService.response(registry.supportedCategories)
  }
}

class MockAuthorization(private val authorization: Map[(String, String), Seq[DataCategory]]) extends AuthorizationService {
  override def authorizeRead(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(who, citizen, category)

  override def authorizeWrite(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(who, citizen, category)

  override def authorizedReadCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(who, citizen)

  override def authorizedWriteCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(who, citizen)

  private def checkRW(authenticated: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val response = authorization.get((authenticated.identifier, citizen))
      .flatMap(categories => checkPermission(categories, category))
      .map(Response(_))
      .getOrElse(Fail(Unauthorized(s"Cannot access to ${category.name} data of citizen $citizen")))

    FutureService(response)
  }

  private def authorizedCategories(authenticated: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = {
    authorization.get((authenticated.identifier, citizen)) match {
      case Some(value) => FutureService.response(value)
      case None => FutureService.fail(Unauthorized(s"User $authenticated cannot access to $citizen"))
    }
  }

  private def checkPermission(authorizeCategories: Seq[DataCategory], requestCategory: DataCategory): Option[DataCategory] = requestCategory match {
    case GroupCategory(name, _) => authorizeCategories.find(_.name == name)
    case LeafCategory(name, _) => DataCategoryOps.allChild(requestCategory).find(leaf => leaf.name == name)
  }
}
