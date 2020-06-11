package it.unibo.core.data

/**
 * main abstraction to store information about a citizen, institution or someone else.
 * It is piece of information characterized by
 *  - identifier : to identify the data uniquely;
 *  - feeder : the things / people that produce the information;
 *  - category : to give semantics to the data;
 *  - timestamp : to define the moment when the information has been produced;
 *  - value : the real data produced.
 */
trait Data {
  def identifier: String
  def feeder : Feeder
  def category : LeafCategory
  def timestamp : Long
  def value : Any
}

object Data {
  def apply(identifier : String, feeder: Feeder, category: LeafCategory, timestamp: Long, value : Any) : Data = {
    AnyData(identifier, feeder, category, timestamp, value)
  }

  /**
   * a Data used to wrap a generic value.
   */
  case class AnyData(identifier : String, feeder: Feeder, category: LeafCategory, timestamp: Long, value : Any) extends Data
}