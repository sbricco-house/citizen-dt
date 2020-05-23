package it.unibo.core.dt

import it.unibo.core.data.Data

object History {
  type History = Seq[Data]
  def History(data: Seq[Data]) = Seq(data)
  def apply(data: Seq[Data]): History = data
}
