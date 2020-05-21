package it.unibo.service.citizen.matcher

import io.vertx.core.json.JsonObject
import io.vertx.lang.scala.json.JsonArray
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

trait DataJsonMatcher {
  self: Matchers =>

  /**
   * Match two Data's content in json representation consider to ignore the id
   */
  def sameData(expectedData: JsonObject) = new DataJsonContent(expectedData)

  /**
   * Match two Data's array content in json representation consider to ignore the id
   */
  def sameArrayData(expectedData: JsonArray) = new DataJsonArrayContent(expectedData)

  protected class DataJsonContent(val right: JsonObject) extends Matcher[JsonObject] {
    override def apply(left: JsonObject): MatchResult = {
      right.remove("id")
      left.remove("id")
      MatchResult(
        left == right,
        s"$left didn't match $right",
        s"$left matched $right"
      )
    }
  }

  protected class DataJsonArrayContent(val right: JsonArray) extends Matcher[JsonArray] {
    override def apply(left: JsonArray): MatchResult = {
      val allMatch = if(left.size() == right.size()) {
        toJsonObjectArray(left).zip(toJsonObjectArray(right)).map {
          case (l, r) => new DataJsonContent(r).apply(l)
        }.forall(_.matches)
      } else false

      MatchResult(
        allMatch,
        s"$left didn't match $right",
        s"$left matched $right"
      )
    }

    private def toJsonObjectArray(array: JsonArray): Seq[JsonObject] = {
      val elems = array.size() - 1
      (0 to elems).map(array.getJsonObject)
    }
  }
}
