package it.unibo.core.data

/**
 * this concept is is used to create an "high" level taxonomy associated to the data category:
 */
sealed trait DataCategory {
  def name : String
}

/**
 * group a set of linked category. (e.g. MedicalData groups a set of data category linked to medical information)
 * @param name
 * @param dataCategory : a set of category related to this one
 */
case class GroupCategory(name : String, dataCategory: Set[DataCategory]) extends DataCategory

/**
 * a leaf category, described an "atomic" information depending on domain (e.g. HeartBeatCategory)
 * @param name
 * @param TTL: a temporal information to the lifespan.
 */
case class LeafCategory(name : String, TTL : Int) extends DataCategory

object DataCategoryOps {
  def allChild(dataCategory: DataCategory) : Set[LeafCategory] = dataCategory match {
    case value : LeafCategory => Set(value)
    case GroupCategory(name, categories) => categories.flatMap(allChild)
  }
}

