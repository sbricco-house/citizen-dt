package it.unibo.service.citizen.authorization
import it.unibo.core.data.DataCategory

import scala.concurrent.Future

object MockAuthorization {
  def apply(authorizedCategories: DataCategory*): MockAuthorization = new MockAuthorization(authorizedCategories)
}

class MockAuthorization private(private val authorizedCategories: Seq[DataCategory]) extends AuthorizationService {

  override def authorizeRead(authenticated: String, citizen: String, category: DataCategory): Future[DataCategory] = mockAuthorize(authenticated, citizen, category)
  override def authorizeWrite(authenticated: String, citizen: String, category: DataCategory): Future[DataCategory] = mockAuthorize(authenticated, citizen, category)
  override def authorizedReadCategories(authenticated: String, citizen: String): Future[Seq[DataCategory]] = mockAuthorizedCategories()
  override def authorizedWriteCategories(authenticated: String, citizen: String): Future[Seq[DataCategory]] = mockAuthorizedCategories()


  private def mockAuthorize(authenticated: String, citizen: String, category: DataCategory) : Future[DataCategory] =
    authorizedCategories.find(_.name == category.name) match {
      case Some(authorized) => Future.successful(authorized)
      case _ => Future.failed(new IllegalAccessException(s"Cannot access to ${category.toString} data of citizen $citizen"))
  }
  private def mockAuthorizedCategories() = Future.successful(authorizedCategories)
}
