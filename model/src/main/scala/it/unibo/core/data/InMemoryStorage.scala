package it.unibo.core.data
import scala.util.{Success, Try}

/**
 * A basic storage implementation that store all information in memory
 * @tparam D the data type (e.g Any, Data, ...)
 * @tparam ID the identification type (e.g. UUID, Int,...)
 */
class InMemoryStorage[D, ID] private() extends Storage [D, ID]{
  var memory : Map[ID, D] = Map.empty[ID, D]
  override def store(id : ID, data: D): Try[D] = {
    memory += id -> data
    Success(data)
  }

  override def get(id: ID): Try[Option[D]] = Success(memory.get(id))

  override def find(policy: D => Boolean): Try[Option[D]] = {
    Success(memory.values.find(policy))
  }


  override def findMany(policy: D => Boolean): Try[Seq[D]] = findMany(policy, InMemoryStorage.NO_LIMIT)

  override def findMany(policy: D => Boolean, maxElements: Int): Try[Seq[D]] = {
    Success(memory.values.filter(policy).take(maxElements).toSeq)
  }
}

object InMemoryStorage {
  def apply[D, ID]() : Storage[D, ID] = new InMemoryStorage()

  private val NO_LIMIT = -1
}
