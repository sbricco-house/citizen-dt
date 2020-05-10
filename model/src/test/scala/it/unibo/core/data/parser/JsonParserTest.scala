package it.unibo.core.data.parser
import io.circe.Json
import io.circe.parser
import it.unibo.core.data.{Data, Feeder, LeafCategory, Resource, Sensor}
import it.unibo.core.parser.JsonParser
import org.scalatest._


class JsonParserTest extends FlatSpec with Matchers {
  import JsonParserTest._

  "A json parser" should "decode a json string" in {
    val parsingResult = parser.parse(inputString) match {
      case Left(failure) => None
      case Right(value) => integerParser.decode(value)
    }
    assert(parsingResult.contains(inputData))
  }

  "A json parser" should "encode a json string" in {
    val parsingResult = integerParser.encode(inputData)
    parser.parse(inputString) match {
      case Left(failure) => fail(failure)
      case Right(value) => assert(parsingResult.contains(value))
    }
  }

  "A json parser" should "parse a json string with resource feeder" in {
    val parsingResult = parser.parse(inputStringUri) match {
      case Left(failure) => None
      case Right(value) => integerParser.decode(value)
    }

    parsingResult.exists(_.feeder == feederResource)
  }
}

object JsonParserTest {
  val integerCategory = LeafCategory("integer", 100)
  case class IntegerData(value : Int, feeder : Feeder, URI : String, timestamp : Long) extends Data {
    val category: LeafCategory = integerCategory
  }
  val integerParser = new JsonParser {
    override protected def createDataFrom(uri: String, feeder: Feeder, timestamp: Long, json: Json): Option[Data] = {
      json.asNumber.map(_.truncateToInt).map(IntegerData(_, feeder, uri, timestamp))
    }
    override protected def encodeStrategy(value: Any): Option[Json] = value match {
      case x : Int => Some(Json.fromInt(x))
      case _ => None
    }

    val target: LeafCategory = integerCategory
  }
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
          "uri" : "health"
          "isResource" : "true"
        },
        "uri" : "citizen",
        "timestamp" : 102,
        "category" : "integer",
        "value" : 10
      }
      """.stripMargin
  val inputData = IntegerData(10, Sensor("health"), "citizen", 102)

  val feederResource = Resource("health")
}