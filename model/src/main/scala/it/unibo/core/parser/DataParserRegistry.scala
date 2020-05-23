package it.unibo.core.parser
import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, DataCategory, GroupCategory, LeafCategory}

// TODO: review this component again..
trait DataParserRegistry[Raw] extends DataParser[Raw] {
  def registerGroupCategory(groupCategory : GroupCategory) : DataParserRegistry[Raw]

  def registerParser(dataParser : DataParser[Raw]) : DataParserRegistry[Raw]

  def decodeCategory(category : String) : Option[DataCategory]
}

object DataParserRegistry {
  def apply[Raw](): DataParserRegistry[Raw] = DataParserRegistryImpl[Raw]()

  private case class DataParserRegistryImpl[Raw](categoryEncoder : Map[String, DataCategory] = Map.empty[String, DataCategory],
                                            dataParserRegistry : Seq[DataParser[Raw]] = Seq.empty[DataParser[Raw]]) extends DataParserRegistry[Raw] {

    override def decode(rawData: Raw): Option[Data] = dataParserRegistry
      .map(_.decode(rawData))
      .collectFirst { case Some(value) => value}


    override def encode(data: Data): Option[Raw] = dataParserRegistry
      .map(_.encode(data))
      .collectFirst { case Some(value) => value }

    override def registerGroupCategory(groupCategory: GroupCategory): DataParserRegistry[Raw] = {
      val categoryEncoderUpdated = this.categoryEncoder + (groupCategory.name -> groupCategory)
      this.copy(categoryEncoder = categoryEncoderUpdated)
    }

    def registerParser(dataParser : DataParser[Raw]) : DataParserRegistry[Raw] = {
      val categoryEncoderUpdated = this.categoryEncoder ++ dataParser.supportedCategories.map(category => category.name -> category)
      this.copy(categoryEncoderUpdated, dataParserRegistry = this.dataParserRegistry :+ dataParser)
    }

    override def decodeCategory(category: String): Option[DataCategory] = this.categoryEncoder.get(category)

    override def supportedCategories: Seq[LeafCategory] = this.categoryEncoder.values.collect { case cat : LeafCategory => cat }.toSeq
  }
}


