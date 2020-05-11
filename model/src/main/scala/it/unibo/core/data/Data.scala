package it.unibo.core.data

import java.util.UUID

/**
 * main abstraction to store information about a citizen, institution or someone else.
 * It is piece of information characterized by
 *  - feeder : the things / people that produce the information;
 *  - URI : to identify the data uniquely;
 *  - category : to give semantics to the data;
 *  - timestamp : to define the moment when the information has been produced;
 *  - value : the real data produced.
 */
trait Data {
  def feeder : Feeder
  def category : LeafCategory
  def timestamp : Long
  def value : Any
}