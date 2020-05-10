package it.unibo.core.parser

import it.unibo.core.data.{Data, LeafCategory}

trait DataParser[Raw] {
  def decode(rawData : Raw) : Option[Data]
  def encode(data : Data) : Option[Raw]
  def target : LeafCategory
}
