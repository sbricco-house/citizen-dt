package it.unibo.core.data.parser

import java.util.UUID

import it.unibo.core.data.{Data, Feeder, LeafCategory, Resource, Sensor}

object JsonElements {
  val integerCategory = LeafCategory("integer", 100)
  case class IntegerData(identifier: String, value : Int, feeder : Feeder, timestamp : Long) extends Data {
    val category: LeafCategory = integerCategory
  }
  val uri = "citizen"
  val inputString =
    """
      {
        "feeder" : {
          "name" : "health",
          "isResource" : false
        },
        "id" : "4ea103ea-0edb-4994-8fa6-e2609b7f610d",
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
        "id" : "4ea103ea-0edb-4994-8fa6-e2609b7f610d",
        "timestamp" : 102,
        "category" : "integer",
        "value" : 10
      }
      """.stripMargin
  val inputData = IntegerData("4ea103ea-0edb-4994-8fa6-e2609b7f610d", 10, Sensor("health"), 102)

  val feederResource = Resource("health")
}
