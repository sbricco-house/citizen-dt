package it.unibo.core.parser
import it.unibo.core.data.{Data, DataCategory, LeafCategory}

// TODO: review this component
trait DataParserRegistry[Raw] extends DataParser[Raw] {
  def registryParser(parser: DataParser[Raw]): Unit
}

object DataParserRegistry {
  def apply[Raw](parsers: DataParser[Raw]*): DataParserRegistry[Raw] = new DefaultDataParserRegistry(parsers.toList)

  class DefaultDataParserRegistry[Raw](private var parserRegistry: List[DataParser[Raw]]) extends DataParserRegistry[Raw] {
    def registryParser(parser: DataParser[Raw]): Unit = parserRegistry ++= List(parser)
    override def decode(rawData: Raw): Option[Data] = parserRegistry.map(_.decode(rawData)).collectFirst { case Some(value) => value }
    override def encode(data: Data): Option[Raw] = parserRegistry.map(_.encode(data)).collectFirst { case Some(value) => value }
    override def target: LeafCategory = LeafCategory("all", -1) // TODO: replace it
  }
}


