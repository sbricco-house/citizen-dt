package it.unibo.service.citizen.authorization
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.{Fail, FutureService, Response}
import it.unibo.core.utils.ServiceError.Unauthorized

import scala.concurrent.Future

object MockAuthorization {
  def apply(authorizedCategories: DataCategory*): MockAuthorization = new MockAuthorization(authorizedCategories)
}

class MockAuthorization private(private val authorizedCategories: Seq[DataCategory]) extends AuthorizationService {

  override def authorizeRead(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] = mockAuthorize(authenticated, citizen, category)
  override def authorizeWrite(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] = mockAuthorize(authenticated, citizen, category)
  override def authorizedReadCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] = mockAuthorizedCategories()
  override def authorizedWriteCategories(authenticated: String, citizen: String): FutureService[Seq[DataCategory]] = mockAuthorizedCategories()


  private def mockAuthorize(authenticated: String, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val response = authorizedCategories.find(_.name == category.name) match {
      case Some(authorized) => Response(authorized)
      case _ => Fail(Unauthorized(s"Cannot access to ${category.toString} data of citizen $citizen"))
    }
    Future.successful(response).toFutureService
  }

  private def mockAuthorizedCategories(): FutureService[Seq[DataCategory]] =
    Future.successful(Response(authorizedCategories)).toFutureService
}
