package it.unibo.service.permission

import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.data.{DataCategory, DataCategoryOps, GroupCategory, LeafCategory}
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized

object MockAuthorization {
  def apply(authorization: Map[(String, String), Seq[DataCategory]]): MockAuthorization = new MockAuthorization(authorization)

  def acceptAll(registry : DataParserRegistry[_]) : AuthorizationService = new AuthorizationService {
    override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = FutureService.response(category)
    override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = FutureService.response(category)
    override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = FutureService.response(registry.supportedCategories)
    override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = FutureService.response(registry.supportedCategories)
  }
  /*
  TODO pensa come fare il role based
  def roleBased(citizenWritePermission : Set[DataCategory], otherWritePermission : Map[String, Set[DataCategory]], otherReadPermission : Map[String, Set[DataCategory]], allCategories : DataCategory *) = new AuthorizationService {
    override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = who match {
      case `citizen` => FutureService.response(category)
      case other => checkRWOther(otherReadPermission, other, category)
    }
    override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = who match {
      case `citizen` if citizenWritePermission.contains(category) => FutureService.response(category)
      case `citizen` => FutureService.fail(Unauthorized(s"User $citizen cannot access to $citizen"))
      case other => checkRWOther(otherWritePermission, other, category)
    }
    override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = who match {
      case `citizen` => FutureService.response(allCategories)
      case other =>getCategoriesFromOther(otherReadPermission, other)
    }
    override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = who match {
      case `citizen` => FutureService.response(citizenWritePermission.toSeq)
      case other => getCategoriesFromOther(otherWritePermission, other)
    }

    private def checkRWOther(mapPermission : Map[String, Set[DataCategory]], who : TokenIdentifier, category : DataCategory) : FutureService[DataCategory] = {
      mapPermission.get(who).filter(isOperationEnabled(_, category)) match {
        case None => FutureService.fail(Unauthorized(s"User $who cannot access to"))
        case Some(_) => FutureService.response(category)
      }
    }

    private def isOperationEnabled(authorizeCategories: Set[DataCategory], requestCategory: DataCategory): Boolean = requestCategory match {
      case GroupCategory(name, _) => authorizeCategories.exists(_.name == name)
      case LeafCategory(name, _) => DataCategoryOps.allChild(requestCategory).exists(leaf => leaf.name == name)
    }

    private def getCategoriesFromOther(mapPermission : Map[String, Set[DataCategory]], who : TokenIdentifier) : FutureService[Seq[DataCategory]] = {
      mapPermission
        .get(who)
        .map(_.toSeq)
        .map(FutureService.response)
        .getOrElse(FutureService.fail(Unauthorized(s"User $who cannot access")))
    }
  }
  */
}

class MockAuthorization(private val authorization: Map[(String, String), Seq[DataCategory]]) extends AuthorizationService {
  override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(who.token, citizen, category)

  override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(who.token, citizen, category)

  override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(who.token, citizen)

  override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(who.token, citizen)

  private def checkRW(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val response = authorization.get((authenticated, citizen))
      .flatMap(categories => checkPermission(categories, category))
      .map(Response(_))
      .getOrElse(Fail(Unauthorized(s"Cannot access to ${category.name} data of citizen $citizen")))

    FutureService(response)
  }

  private def authorizedCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] = {
    authorization.get((authenticated, citizen)) match {
      case Some(value) => FutureService.response(value)
      case None => FutureService.fail(Unauthorized(s"User $authenticated cannot access to $citizen"))
    }
  }

  private def checkPermission(authorizeCategories: Seq[DataCategory], requestCategory: DataCategory): Option[DataCategory] = requestCategory match {
    case GroupCategory(name, _) => authorizeCategories.find(_.name == name)
    case LeafCategory(name, _) => DataCategoryOps.allChild(requestCategory).find(leaf => leaf.name == name)
  }
}
