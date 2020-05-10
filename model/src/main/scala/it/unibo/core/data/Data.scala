package it.unibo.core.data

trait Data {
  def feeder : Feeder
  def URI : String
  def category : LeafCategory
  def timestamp : Long
  def value : Any
}