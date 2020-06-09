package it.unibo.core.data

/**
 * it describe a feeder of information
 */
sealed trait Feeder

/**
 * a feeder can be a resource (according to the REST semantics)
 * @param URI: ID associated to the resource that produce the data
 */
case class Resource(URI : String) extends Feeder

//TODO add owned by in sensor.
/**
 * a feeder can be a generic sensor that produce some information
 * @param name 
 */
case class Sensor(name : String) extends Feeder
