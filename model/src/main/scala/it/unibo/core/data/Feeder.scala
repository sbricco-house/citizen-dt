package it.unibo.core.data

sealed trait Feeder

case class Resource(URI : Feeder) extends Feeder

case class Sensor(name : String) extends Feeder
