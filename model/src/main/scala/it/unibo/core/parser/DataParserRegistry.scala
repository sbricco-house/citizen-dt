package it.unibo.core.parser
import it.unibo.core.data.{Data, DataCategory, GroupCategory, LeafCategory}

trait DataParserRegistry[External] extends DataParser[External] {
  def registerGroupCategory(groupCategory : GroupCategory) : DataParserRegistry[External]

  def registerParser(dataParser : DataParser[External]) : DataParserRegistry[External]

  def decodeCategory(category : String) : Option[DataCategory]
}

object DataParserRegistry {
  def apply[External](): DataParserRegistry[External] = DataParserRegistryImpl[External]()

  private case class DataParserRegistryImpl[External](categoryEncoder : Map[String, DataCategory] = Map.empty[String, DataCategory],
                                                      dataParserRegistry : Seq[DataParser[External]] = Seq.empty[DataParser[External]]) extends DataParserRegistry[External] {

    override def decode(rawData: External): Option[Data] = dataParserRegistry
      .map(_.decode(rawData))
      .collectFirst { case Some(value) => value}


    override def encode(data: Data): Option[External] = dataParserRegistry
      .map(_.encode(data))
      .collectFirst { case Some(value) => value }

    override def registerGroupCategory(groupCategory: GroupCategory): DataParserRegistry[External] = {
      val categoryEncoderUpdated = this.categoryEncoder + (groupCategory.name -> groupCategory)
      this.copy(categoryEncoder = categoryEncoderUpdated)
    }

    def registerParser(dataParser : DataParser[External]) : DataParserRegistry[External] = {
      val categoryEncoderUpdated = this.categoryEncoder ++ dataParser.supportedCategories.map(category => category.name -> category)
      this.copy(categoryEncoderUpdated, dataParserRegistry = this.dataParserRegistry :+ dataParser)
    }

    override def decodeCategory(category: String): Option[DataCategory] = this.categoryEncoder.get(category)

    override def supportedCategories: Seq[LeafCategory] = this.categoryEncoder.values.collect { case cat : LeafCategory => cat }.toSeq
  }
}


