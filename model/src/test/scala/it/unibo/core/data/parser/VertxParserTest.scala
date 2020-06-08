package it.unibo.core.data.parser

import java.util.UUID

import io.vertx.lang.scala.json.{Json, JsonObject}
import it.unibo.core.data.{Data, Feeder, LeafCategory}
import it.unibo.core.parser.VertxJsonParser
import org.scalatest._
import org.scalatest.matchers.should.Matchers

class VertxParserTest extends FlatSpec with Matchers {

  import JsonElements._
  import VertxParserTest._
  "A vertx json parser" should "decode a json string" in {
    val json = Json.fromObjectString(inputString)
    val result = integerParser.decode(json)
    assert(result.contains(inputData))
  }

  "A vertx json parser" should "encode a json string" in {
    val parsingResult = integerParser.encode(inputData)

    val json = Json.fromObjectString(inputString)

    assert(parsingResult.contains(json))
  }

  "A vertx json parser" should "parse a json string with resource feeder" in {
    val json = Json.fromObjectString(inputString)
    val result = integerParser.decode(json)
    result.exists(_.feeder == feederResource)
  }
}

object VertxParserTest {
  import JsonElements._
  import it.unibo.core.microservice.vertx._
  val integerParser = new VertxJsonParser {

    override protected def encodeStrategy(value: Any): Option[JsonObject] = value match {
      case x : Int => Some(Json.emptyObj().put("value", x))
      case _ => None
    }

    override def supportedCategories: Seq[LeafCategory] = List(integerCategory)

    override protected def createDataFrom(identifier: String, feeder: Feeder, timestamp: Long, category: LeafCategory, value: JsonObject): Option[Data] = {
      value.getAsInt("value").map(IntegerData(UUID.fromString(identifier).toString, _, feeder, timestamp))
    }
  }
}

