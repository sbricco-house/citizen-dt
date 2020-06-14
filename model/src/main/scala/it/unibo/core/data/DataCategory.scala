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
 * @param TTL: a temporal information to the lifespan, default -1 -> unlimited.
 */
class LeafCategory(val name : String, val TTL : Long = -1) extends DataCategory {
  def isLimited : Boolean = TTL > 0

  def canEqual(other: Any): Boolean = other.isInstanceOf[LeafCategory]

  override def equals(other: Any): Boolean = other match {
    case that: LeafCategory =>
      (that canEqual this) &&
        name == that.name
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(name)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }


  override def toString = s"LeafCategory($name)"
}

object LeafCategory {
  def apply(name : String, TTL : Long = -1) : LeafCategory = new LeafCategory(name, TTL)
  def unapply(arg: LeafCategory): Option[(String, Long)] = Some((arg.name, arg.TTL))
}

object DataCategoryOps {
  /**
   * extract all child categories from a DataCategory. If the DataCategories is Leaf, returns itself.
   * @param dataCategory the data category to expand
   * @return all child starting form data category
   */
  def allChild(dataCategory: DataCategory) : Set[LeafCategory] = dataCategory match {
    case value : LeafCategory => Set(value)
    case GroupCategory(name, categories) => categories.flatMap(allChild)
  }
  def contains(category1: DataCategory, category2: DataCategory): Option[DataCategory] = (category1, category2) match {
    case (GroupCategory(_, set), LeafCategory(name, _)) => set find(_.name == name)
    case (GroupCategory(name1, set1), GroupCategory(name2, _)) if name1 == name2 => Some(GroupCategory(name1, set1))
    case (LeafCategory(name1, ttl), LeafCategory(name2, _)) if name1 == name2 => Some(LeafCategory(name1, ttl))
    case _ => None
  }
}

