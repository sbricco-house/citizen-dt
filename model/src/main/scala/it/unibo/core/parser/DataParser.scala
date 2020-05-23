package it.unibo.core.parser

import it.unibo.core.data.{Data, LeafCategory}

/**
 * This concept allow to marshall and marshalling information expressed in some Raw codification.
 * It has the responsibility to deconde/encode a set of data category
 * @tparam Raw the type of codification used to store data externally (e.g. Json, Xml,String,..)
 */
trait DataParser[Raw] extends Parser[Data, Raw] {
  override type E[O] = Option[Data]
  def decode(rawData : Raw) : Option[Data]
  def supportedCategories : Seq[LeafCategory]
  def encode(data : Data) : Option[Raw]
}
