package it.unibo.service.permission

import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}

/**
 * The service logic used to manage the permission to a citizen state.
 * This logic is a subpart of a more complex authorization microservice (e.g. contract stipula,..)
 */
trait AuthorizationService {
  /**
   * It tells if who can read the data mark with category in citizen
   * @param who The system used (express by token) that want to read a data
   * @param citizen The citizen that own the data
   * @param category The category of the data
   * @return Response(category) if who can read the data, Fail otherwise
   */
  def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory]
  /**
   * It tells if who can write the data mark with category in citizen
   * @param who The system used (express by token) that want to write a data
   * @param citizen The citizen in which who write the data mark with data category
   * @param category The category of the data
   * @return Response(category) if who can write the data, Fail otherwise
   */
  def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory]
  /**
   * It returns all data category that who can read from citizen
   * @param who The system used (express by token) that want to write a data
   * @param citizen The citizen that own the data
   * @return Response(Seq(categories))
   */
  def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]]
  /**
   * It returns all data category that who can write in citizen
   * @param who The system used (express by token) that want to write a data
   * @param citizen  The citizen in which who write the data mark with some data category
   * @return Response(Seq(categories))
   */
  def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]]
}