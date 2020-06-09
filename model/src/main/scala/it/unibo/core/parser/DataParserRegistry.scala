package it.unibo.core.parser
import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, DataCategory, GroupCategory, LeafCategory}

/**
 * this is a registry that contains DataParser used to encode / decode multiple Data categories.
 * This registry allow also to marshall and unmarshall DataCategory objects.
 * @tparam External the type of codification used to store data externally (e.g. Json, Xml,String,..)
 */
trait DataParserRegistry[External] extends DataParser[External] {
  /**
   * Add a group category in the registry
   * @param groupCategory
   * @return A new DataParserRegistry enabled to parser groupCategory
   */
  def registerGroupCategory(groupCategory : GroupCategory) : DataParserRegistry[External]

  /**
   * Add a dataParser in the registry
   * @param dataParser
   * @return a new DataParserRegistry that used dataParser to decode/encode Data.
   */
  def registerParser(dataParser : DataParser[External]) : DataParserRegistry[External]

  /**
   * decode a category from a string. To encode a category it search:
   *  - from all data category group registered
   *  - from all dataParser supported category
   * @param category : The category name
   * @return Some(category) if the registry find the category, None otherwise
   */
  def decodeCategory(category : String) : Option[DataCategory]
}

object DataParserRegistry {
  val emptyJson : DataParserRegistry[JsonObject] = DataParserRegistryImpl()

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


