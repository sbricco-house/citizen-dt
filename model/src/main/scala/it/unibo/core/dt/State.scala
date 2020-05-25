package it.unibo.core.dt

import it.unibo.core.data.{Data, DataCategory, DataCategoryOps, LeafCategory}

/**
 * generic abstraction to define a digital twin state.
 * A digital twin has a set of data characterized by a category.
 */
trait State {
  /**
   * get the data associated to a data category. If the data category is Group Category,
   * a sequence of data can be returned
   * @param category
   * @return : Seq.empty if there are data linked to the category, Seq(..) of linked data otherwise
   */
  def get(category : DataCategory) : Seq[Data]

  def get(dataCategories: Seq[DataCategory]): Seq[Data] = dataCategories.flatMap(this.get).distinct

  /**
   * update the current state of DT with a new Data
   * @param data
   * @return a new snapshot of the state.
   */
  def update(data : Data) : State
  //TODO pensa se toglierlo o meno
  /**
   * get entire view of the digital twin state
   * @return
   */
  def snapshot : Seq[Data]
}

object State {
  private case class MapLikeState(map : Map[LeafCategory, Data]) extends State {
    override def get(category: DataCategory): Seq[Data] ={
      DataCategoryOps.allChild(category)
        .map(cat => map.get(cat))
        .collect{
          case Some(m) => m
        }
        .toSeq
    }
    override def update(data: Data): State = map.get(data.category) match {
      case Some(old) if old.timestamp > data.timestamp => this
      case _ => MapLikeState(map + (data.category -> data))
    }
    override def snapshot: Seq[Data] = map.values.toSeq
  }
  val empty : State = MapLikeState(Map.empty)
}
