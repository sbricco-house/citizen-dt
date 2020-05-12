package it.unibo.service.citizen.authorization
import it.unibo.core.data.DataCategory

import scala.concurrent.Future

object MockAuthorization {
  def apply(authorizedCategories: DataCategory*): MockAuthorization = new MockAuthorization(authorizedCategories)
}

class MockAuthorization private(private val authorizedCategories: Seq[DataCategory]) extends AuthorizationFacade {

  override def authorizeRead(authenticated: String, citizen: String, category: DataCategory): Future[Option[DataCategory]] = mockAuthorize(authenticated, citizen, category)
  override def authorizeWrite(authenticated: String, citizen: String, category: DataCategory): Future[Option[DataCategory]] = mockAuthorize(authenticated, citizen, category)
  override def authorizedReadCategories(authenticated: String, citizen: String): Future[Seq[DataCategory]] = mockAuthorizedCategories()
  override def authorizedWriteCategories(authenticated: String, citizen: String): Future[Seq[DataCategory]] = mockAuthorizedCategories()


  private def mockAuthorize(authenticated: String, citizen: String, category: DataCategory) : Future[Option[DataCategory]] = Future.successful(authorizedCategories.find(_.name == category.name))
  private def mockAuthorizedCategories() = Future.successful(authorizedCategories)
}
