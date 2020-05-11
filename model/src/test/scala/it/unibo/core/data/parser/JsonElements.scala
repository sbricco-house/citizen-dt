package it.unibo.core.data.parser

import it.unibo.core.data.{Data, Feeder, LeafCategory, Resource, Sensor}

object JsonElements {
  val integerCategory = LeafCategory("integer", 100)
  case class IntegerData(value : Int, feeder : Feeder, timestamp : Long) extends Data {
    val category: LeafCategory = integerCategory
  }
  val uri = "citizen"
  val inputString =
    """
      {
        "feeder" : {
          "name" : "health"
        },
        "uri" : "citizen",
        "timestamp" : 102,
        "category" : "integer",
        "value" : 10
      }
      """.stripMargin

  val inputStringUri =
    """
      {
        "feeder" : {
          "uri" : "health",
          "isResource" : "true"
        },
        "uri" : "citizen",
        "timestamp" : 102,
        "category" : "integer",
        "value" : 10
      }
      """.stripMargin
  val inputData = IntegerData(10, Sensor("health"), 102)

  val feederResource = Resource("health")
}
