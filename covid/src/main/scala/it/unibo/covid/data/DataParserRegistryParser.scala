package it.unibo.covid.data

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.data.{GroupCategory, LeafCategory}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ParserLike.Parser
import it.unibo.core.parser.ValueParser.ValueParser
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}
import it.unibo.covid.data.DataParserRegistryParser.ValueParserMap
//TODO documentation
object DataParserRegistryParser {
  type ValueParserMap = String => Option[ValueParser[JsonObject]]
  def apply(valueParserMap: ValueParserMap) = new DataParserRegistryParser(valueParserMap)
}

class DataParserRegistryParser(valueParserMap: ValueParserMap) extends Parser[JsonArray, DataParserRegistry[JsonObject]] {

  private case class DataCategorySection(name: String, ttl: Int, parseType: String, groups: Seq[String])

  override def encode(rawData: DataParserRegistry[JsonObject]): JsonArray = ??? // TODO: implement but is not necessary

  override def decode(data: JsonArray): Option[DataParserRegistry[JsonObject]] = {
    val categories = extractDataCategorySections(data)
    val leafCategoryEntries = extractLeafEntries(categories, valueParserMap)
    val groupCategoryEntries = extractGroupEntries(categories, leafCategoryEntries.values.flatten.toSeq)

    var registry = DataParserRegistry[JsonObject]()
    leafCategoryEntries.foreach { t =>
      registry = registry.registerParser(VertxJsonParser(t._1, t._2: _*))
    }
    groupCategoryEntries.foreach { g =>
      registry = registry.registerGroupCategory(g)
    }
    Some(registry)
  }

  private def extractDataCategorySections(json: JsonArray): Seq[DataCategorySection] = {
    val items = json.getAsObjectSeq.getOrElse(Seq(Json.emptyObj()))
    for {
      obj <- items
      name <- obj.getAsString("name")
      ttl <- obj.getAsInt("ttl")
      parserType <- obj.getAsString("type")
      groups <- obj.getAsArray("groups")
    } yield DataCategorySection(name, ttl, parserType, groups.getAsStringSeq.getOrElse(Seq()))
  }

  private def extractLeafEntries(sections: Seq[DataCategorySection], valueParserMap: ValueParserMap): Map[ValueParser[JsonObject], Seq[LeafCategory]] = {
    sections.flatMap(s => valueParserMap(s.parseType).map(parser => (parser, LeafCategory(s.name, s.ttl))))
      .foldRight(Map[ValueParser[JsonObject], Seq[LeafCategory]]()) {
        case ((parser, category), acc) => addWithoutCollision(acc, parser, category)
      }
  }

  private def extractGroupEntries(sections: Seq[DataCategorySection], leafSupported: Seq[LeafCategory]): Seq[GroupCategory] = {
    def getManyLeafsByName(supported: Seq[LeafCategory], categoryNames: Seq[String]): Seq[LeafCategory] = {
      categoryNames.flatMap(leaf => supported.find(sl => sl.name == leaf))
    }

    sections.filter(s => leafSupported.exists(l => l.name == s.name))
      .flatMap(s => s.groups.map(g => (s.name, g)))
      .foldRight(Map[String, Seq[String]]()) {
        case ((category, group), acc) => addWithoutCollision(acc, group, category)
      }
      .mapValues(leafs => getManyLeafsByName(leafSupported, leafs))
      .map(kv => GroupCategory(kv._1, kv._2.toSet))
      .toSeq
  }

  private def addWithoutCollision[K, V](map: Map[K, Seq[V]], key: K, value: V): Map[K, Seq[V]] = {
    val sequence = if(map.contains(key)) map(key) ++ Seq(value) else Seq(value)
    map + (key -> sequence)
  }
}