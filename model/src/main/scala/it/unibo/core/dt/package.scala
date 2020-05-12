package it.unibo.core

import it.unibo.core.data.{Data, DataCategory}

package object dt {
  implicit class RichState(state: State) {
    def get(dataCategories: Seq[DataCategory]): Seq[Data] = {
      dataCategories.flatMap(state.get).distinct
    }
  }
}
