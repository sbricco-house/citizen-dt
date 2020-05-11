package it.unibo.core.parser

import it.unibo.core.data.{Data, LeafCategory}

/**
 * This concept allow to marshall and marshalling information expressed in some Raw codification.
 * It has the responsibility to deconde/encode only a specific type of data category (target)
 * @tparam Raw the type of codification used to store data externally (e.g. Json, Xml,String,..)
 */
trait DataParser[Raw] {
  def decode(rawData : Raw) : Option[Data]
  def encode(data : Data) : Option[Raw]
  def target : LeafCategory
}
