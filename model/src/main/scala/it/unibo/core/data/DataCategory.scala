package it.unibo.core.data

sealed trait DataCategory {
  def name : String
}

case class GroupCategory(name : String, dataCategory: DataCategory *) extends DataCategory
case class LeafCategory(name : String, TTL : Int) extends DataCategory

