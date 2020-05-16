package it.unibo.service.permission
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService

class AuthorizationClient extends AuthorizationService {

  override def authorizeRead(who: String, citizen: String, category: DataCategory): FutureService[DataCategory] = ???

  override def authorizeWrite(who: String, citizen: String, category: DataCategory): FutureService[DataCategory] = ???

  override def authorizedReadCategories(who: String, citizen: String): FutureService[Seq[DataCategory]] = ???

  override def authorizedWriteCategories(who: String, citizen: String): FutureService[Seq[DataCategory]] = ???

}
