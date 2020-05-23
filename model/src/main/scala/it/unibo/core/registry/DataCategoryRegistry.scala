package it.unibo.core.registry

import it.unibo.core.data.{DataCategory, GroupCategory, LeafCategory}

/**
 * Abstraction of registry where save all supported categories by the system
 */
trait DataCategoryRegistry {
  def register(category: DataCategory): Unit
  def unregister(categoryName: String): Unit
  def get(categoryName: String): Option[DataCategory]
}

object DataCategoryRegistry {
  def apply(): DataCategoryRegistry = new MapDataCategoryRegistry(Map())

  private class MapDataCategoryRegistry(var registry: Map[String, DataCategory]) extends DataCategoryRegistry {

    override def register(category: DataCategory): Unit = category match {
      case LeafCategory(name, _) => registry += (name -> category)
      case GroupCategory(name, nestedCategories) =>
        registry += (name -> category); nestedCategories.foreach(register)
    }

    override def get(categoryName: String): Option[DataCategory] = registry.get(categoryName)

    override def unregister(categoryName: String): Unit =
      registry -= categoryName
  }
}