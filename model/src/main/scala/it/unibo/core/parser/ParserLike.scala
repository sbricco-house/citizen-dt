package it.unibo.core.parser

/**
 * It describes a general parser that can encode/data data from two representations.
 * @tparam External The "external" representation of data to manage (e.g. Json, XML, String,..)
 * @tparam Internal The "internal" representation (e.g. Data, Any,..)
 */
trait ParserLike[External, Internal] {
  /**
   * Higher kind used to manage both decode function. E is a wrapper of External.
   * E could be Option (case of Missmatchable Parser) or External itself
   * @tparam _
   */
  type E[_]
  def encode(rawData : Internal) : E[External]
  def decode(data : External) : Option[Internal]
}

object ParserLike {

  /**
   * A specialization of ParserLink that can always map a External data into a Raw data
   * @tparam External The "external" representation of data to manage (e.g. Json, XML, String,..)
   * @tparam Internal The "internal" representation (e.g. Data, Any,..)
   */
  trait Parser[External, Internal] extends ParserLike[External, Internal]{
    override type E[O] = External
  }

  /**
   * A specialization of ParserLink that can sometimes map a Internal data into a External data. If it can't
   * encode Raw, returns None and has a missmatch .
   * @tparam External The "external" representation of data to manage (e.g. Json, XML, String,..)
   * @tparam Internal The "internal" representation (e.g. Data, Any,..)
   */
  trait MismatchableParser[External, Internal] extends ParserLike[External, Internal] {
    override type E[O] = Option[O]
  }

  /**
   * Create a parser from decode and encode strategy passed
   * @param encodeFunction The mapping from Internal to External
   * @param decodeFunction The mapping form External to Internal
   * @tparam External The "external" representation of data to manage (e.g. Json, XML, String,..)
   * @tparam Internal The "internal" representation (e.g. Data, Any,..)
   * @return A parser that encode/decode External and Internal data
   */
  def apply[External, Internal](encodeFunction : Internal => External)(decodeFunction : External => Option[Internal]): Parser[External, Internal] = new Parser[External, Internal] {
    override def encode(rawData: Internal): External = encodeFunction(rawData)
    override def decode(data: External): Option[Internal] = decodeFunction(data)
  }

  /**
   * Create a mismatchable parser from decode and encode strategy passed
   * @param encodeFunction The "mismatchable" mapping from Internal to External
   * @param decodeFunction The mapping form External to Internal
   * @tparam External The "external" representation of data to manage (e.g. Json, XML, String,..)
   * @tparam Internal The "internal" representation (e.g. Data, Any,..)
   * @return A mismatchable parser that encode/decode External and Internal data
   */
  def mismatchable[External, Internal](encodeFunction: Internal => Option[External])(decodeFunction : External => Option[Internal])  : MismatchableParser[External, Internal] = {
    new MismatchableParser[External, Internal] {
      override def encode(rawData: Internal): Option[External] = encodeFunction(rawData)
      override def decode(data: External): Option[Internal] = decodeFunction(data)
    }
  }

  /**
   * create a mismatchable parser that fail always encode function.
   * @param decodeFunction The mapping form External to Internal
   * @tparam External The "external" representation of data to manage (e.g. Json, XML, String,..)
   * @tparam Internal The "internal" representation (e.g. Data, Any,..)
   * @return A mismatchable parser that decode External to Internal data
   */
  def decodeOnly[External, Internal](decodeFunction : External => Option[Internal]) : MismatchableParser[External, Internal] = mismatchable((_ : Internal) => None)(decodeFunction)

}
