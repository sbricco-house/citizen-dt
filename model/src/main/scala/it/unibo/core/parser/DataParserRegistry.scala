package it.unibo.core.parser
import io.vertx.lang.scala.json.JsonObject
import it.unibo.core.data.{Data, DataCategory, GroupCategory, LeafCategory}

// TODO: review this component again..
trait DataParserRegistry[Raw] extends DataParser[Raw] {
  def registerGroupCategory(groupCategory : GroupCategory) : DataParserRegistry[Raw]

  def registerParser(valueParser : ValueParser[Raw], categories : LeafCategory *) : DataParserRegistry[Raw]

  def decodeCategory(category : String) : Option[DataCategory]
}

object DataParserRegistry {
  object Json {
    def apply(): DataParserRegistry[JsonObject] = JsonDataParserRegistry()

    private case class JsonDataParserRegistry(categoryEncoder : Map[String, DataCategory] = Map.empty[String, DataCategory],
                                             dataParserRegistry : Seq[DataParser[JsonObject]] = Seq.empty[DataParser[JsonObject]]) extends DataParserRegistry[JsonObject] {

      override def decode(rawData: JsonObject): Option[Data] = dataParserRegistry
        .map(_.decode(rawData))
        .collectFirst { case Some(value) => value}


      override def encode(data: Data): Option[JsonObject] = dataParserRegistry
        .map(_.encode(data))
        .collectFirst { case Some(value) => value }

      override def registerGroupCategory(groupCategory: GroupCategory): DataParserRegistry[JsonObject] = {
        val categoryEncoderUpdated = this.categoryEncoder + (groupCategory.name -> groupCategory)
        this.copy(categoryEncoder = categoryEncoderUpdated)
      }

      def registerParser(valueParser : ValueParser[JsonObject], categories : LeafCategory *) : DataParserRegistry[JsonObject] = {
        val data = VertxJsonParser.apply(valueParser, categories:_*)
        val categoryEncoderUpdated = this.categoryEncoder ++ categories.map(category => category.name -> category)
        this.copy(categoryEncoderUpdated, dataParserRegistry = this.dataParserRegistry :+ data)
      }

      override def decodeCategory(category: String): Option[DataCategory] = this.categoryEncoder.get(category)

      override def supportedCategories: Seq[LeafCategory] = this.categoryEncoder.values.collect { case cat : LeafCategory => cat }.toSeq
    }
  }
}


