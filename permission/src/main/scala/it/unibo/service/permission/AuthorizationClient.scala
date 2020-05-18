package it.unibo.service.permission
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService

class AuthorizationClient extends AuthorizationService {

  override def authorizeRead(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = ???

  override def authorizeWrite(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = ???

  override def authorizedReadCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = ???

  override def authorizedWriteCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = ???

}
