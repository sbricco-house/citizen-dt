package it.unibo.service.citizen

import it.unibo.core.data.{Data, InMemoryStorage, Storage}
import it.unibo.core.dt.State

import scala.util.{Success, Try}

trait HistoryStorage extends Storage[Data, String] {
  def extractState() : Try[State]
}

object HistoryStorage {
  /**
   * USING THIS OBJECT ONLY FOR DEMO APPLICATION
   *
   * @param storage
   * @return
   */
  def fromInMemory(storage : InMemoryStorage[Data, String] = InMemoryStorage()) : HistoryStorage = new HistoryStorage {
    override def extractState(): Try[State] = {
      var state = State.empty
      storage.internalMemory.values.foreach {
        data => state = state.update(data)
      }
      Success(state)
    }
    override def store(id: String, data: Data): Try[Data] = storage.store(id, data)
    override def get(id: String): Try[Option[Data]] = storage.get(id)
    override def find(policy: Data => Boolean): Try[Option[Data]] = storage.find(policy)
    override def findMany(policy: Data => Boolean, maxElements: Int): Try[Seq[Data]] = storage.findMany(policy, maxElements)
  }
}
