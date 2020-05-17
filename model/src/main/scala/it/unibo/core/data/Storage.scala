package it.unibo.core.data

import scala.util.Try

/**
 * an abstraction that defines a generic storage in winch save some data.
 * @tparam D the data type (e.g Any, Data, ...)
 * @tparam ID the identification type (e.g. UUID, Int,...)
 */
trait Storage[D, ID] {
  /**
   * store the associated id -> data into the storage
   * @param id
   * @param data
   * @return Success() if the operation has success, Failure otherwise
   */
  def store(id : ID, data : D) : Try[D]

  /**
   * get the data associated to an ID in the storage, if it is already stored.
   * @param id
   * @return Left(Some(_)) if the data is present, Right(Failure(_)) otherwise
   */
  def get(id : ID) : Try[Option[D]]

  /**
   * get a data that satisfy some logic
   * @param policy: the policy used to find a data
   * @return Left(Some(_)) if there is a data that satisfy the policy, Right(Failure(_)) otherwise
   */
  def find(policy : D => Boolean) : Try[Option[D]]

  def findMany(policy : D => Boolean, maxElements: Int) : Try[Seq[D]]
}

