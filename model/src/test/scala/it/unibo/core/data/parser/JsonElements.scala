package it.unibo.core.data.parser

import java.util.UUID

import it.unibo.core.data.{Data, Feeder, LeafCategory, Resource, Sensor}

object JsonElements {
  val integerCategory = LeafCategory("integer", 100)
  case class IntegerData(id: UUID, value : Int, feeder : Feeder, timestamp : Long) extends Data {
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
  val inputData = IntegerData(UUID.randomUUID(), 10, Sensor("health"), 102)

  val feederResource = Resource("health")
}
