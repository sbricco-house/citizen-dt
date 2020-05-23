package it.unibo.core.parser

trait Parser[Rep, Raw] {
  type E[Rep]
  def decode(rawData : Raw) : E[Rep]
  def encode(data : Rep) : Option[Raw]
}

object Parser {
  def apply[Rep, Raw](decodeFunction : Raw => Rep)(encodeFunction : Rep => Option[Raw]) = new Parser[Rep, Raw] {
    override type E[O] = Rep
    override def decode(rawData: Raw): Rep = decodeFunction(rawData)

    override def encode(data: Rep): Option[Raw] = encodeFunction(data)
  }
}
