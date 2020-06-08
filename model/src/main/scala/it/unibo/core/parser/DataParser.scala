package it.unibo.core.parser

import it.unibo.core.data.{Data, LeafCategory}
import it.unibo.core.parser.ParserLike.MismatchableParser

/**
 * This concept allow to marshall and marshalling information expressed in some Raw codification.
 * It has the responsibility to decode/encode a set of data category
 * @tparam External the type of codification used to store data externally (e.g. Json, Xml,String,..)
 */
trait DataParser[External] extends MismatchableParser[External, Data] {
  /**
   * @return all categories supported by this data parser
   */
  def supportedCategories : Seq[LeafCategory]
}

