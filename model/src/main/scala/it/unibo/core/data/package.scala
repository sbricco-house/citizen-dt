package it.unibo.core

import scala.collection.GenTraversableOnce

package object data {
  /**
   * Implicit that allow to flat a DataCategory to sequence of DataCategory.
   * Generally when need to match two sequence of DataCategory, but we must consider that
   * GroupCategory contains a set of LeafCategory. GroupCategory need to be flatten to a sequence of LeafCategory.
   */
  implicit val flatDataCategory: DataCategory => GenTraversableOnce[DataCategory] = {
    case GroupCategory(_, dataCategory) => dataCategory
    case leaf @ LeafCategory(_, _) => Seq(leaf)
  } // TODO: eval if is right use an implicit or use a map and flatMap directly into DataCategory abstraction
}
