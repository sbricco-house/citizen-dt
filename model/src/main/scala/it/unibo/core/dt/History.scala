package it.unibo.core.dt

import it.unibo.core.data.Data

object History {
  /**
   * type alisas for historical data (a Seq of Data)
   */
  type History = Seq[Data]
  def apply(data: Seq[Data]): History = data
}
