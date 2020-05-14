package it.unibo.service.citizen.authorization

import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.utils.ServiceError.{MissingResource, Unauthorized}

import scala.concurrent.Future

object MockAuthorization {
  def apply(authorization: Map[(String, String), Seq[DataCategory]]): MockAuthorization = new MockAuthorization(authorization)
}

class MockAuthorization (private val authorization: Map[(String, String), Seq[DataCategory]]) extends AuthorizationService {
  override def authorizeRead(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(authenticated, citizen, category)

  override def authorizeWrite(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] =
    checkRW(authenticated, citizen, category)

  override def authorizedReadCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(authenticated, citizen)

  override def authorizedWriteCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] =
    authorizedCategories(authenticated, citizen)

  private def checkRW(authenticated: String, citizen: String, category: DataCategory) = {
    val auth = authorization.get((authenticated, citizen)).map(categories => categories.find(_.name == category.name)) match {
      case Some(Some(value)) => Response(value)
      case Some(None) => Fail(MissingResource(s"Data category ${category.name} not exist"))
      case _ => Fail(Unauthorized(s"Cannot access to ${category.toString} data of citizen $citizen"))
    }
    Future.successful(auth).toFutureService
  }

  private def authorizedCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] =
    authorization.get((authenticated, citizen)) match {
      case Some(value) => FutureService.response(value)
      case None => FutureService.fail(Unauthorized(s"User $authenticated cannot access to $citizen"))
    }
}
