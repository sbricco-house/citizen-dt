package it.unibo.covid.data

import io.vertx.lang.scala.json.{Json, JsonArray, JsonObject}
import it.unibo.core.data.{GroupCategory, LeafCategory}
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.ValueParser.ValueParser
import it.unibo.core.parser.{DataParserRegistry, VertxJsonParser}

object DataParserRegistryFactory {

  type ValueParserMap = String => Option[ValueParser[JsonObject]]

  def fromJson(jsonObject: JsonArray, valueParserMap: ValueParserMap): DataParserRegistry[JsonObject] = {
    val categories = extractDataCategorySections(jsonObject)
    val leafCategoryParsers = extractLeafCategories(categories, valueParserMap)
    val validGroups = extractGroupCategories(categories, leafCategoryParsers.values.flatten.toSeq)

    var registry = DataParserRegistry[JsonObject]()
    leafCategoryParsers.foreach { t =>
      registry = registry.registerParser(VertxJsonParser(t._1, t._2: _*))
    }
    validGroups.foreach { g =>
      registry = registry.registerGroupCategory(g)
    }
    registry
  }

  private case class DataCategorySection(name: String, ttl: Int, parseType: String, groups: Seq[String])

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

  private def extractLeafCategories(sections: Seq[DataCategorySection], valueParserMap: ValueParserMap): Map[ValueParser[JsonObject], Seq[LeafCategory]] = {
    sections.map(s => (s.parseType, LeafCategory(s.name, s.ttl)))
      .flatMap(kv => valueParserMap(kv._1).map(parser => (parser, kv._2)))
      .foldRight(Map[ValueParser[JsonObject], Seq[LeafCategory]]()) {
        case ((parser, category), acc) => addWithoutCollision(acc, parser, category)
      }
  }

  private def extractGroupCategories(sections: Seq[DataCategorySection], leafSupported: Seq[LeafCategory]): Set[GroupCategory] = {
    sections.filter(s => leafSupported.exists(l => l.name == s.name))
      .flatMap(s => s.groups.map(g => (s.name, g)))
      .foldRight(Map[String, Seq[String]]()) {
        case ((category, group), acc) => addWithoutCollision(acc, group, category)
      }
      .mapValues(leafs => leafs.map(leafName => leafSupported.find(_.name == leafName).get))
      .map(kv => GroupCategory(kv._1, kv._2.toSet))
      .toSet
  }

  private def addWithoutCollision[K, V](map: Map[K, Seq[V]], key: K, value: V): Map[K, Seq[V]] = {
    val sequence = if(map.contains(key)) map(key) ++ Seq(value) else Seq(value)
    map + (key -> sequence)
  }
}
