package it.unibo.core.dt

import it.unibo.core.data.{Data, GroupCategory, LeafCategory, Sensor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StateTest extends AnyFlatSpec with Matchers {
  val initialState = State.empty
  import StateTest._
  "State " should "be immutable" in {
    initialState.update(dataA)
    initialState.snapshot.isEmpty shouldBe true
  }

  "State " should "maintain newest data" in {
    val newState = initialState.update(dataANewest).update(dataA)
    newState.get(categoryA) shouldBe Seq(dataANewest)
  }

  "State update " should "create a new snapshot" in {
    val newState = initialState.update(dataA)
    newState.get(categoryA) shouldBe Seq(dataA)
    newState.snapshot shouldBe Seq(dataA)
  }

  "Multipe state update" should "create a consistent snapshot" in {
    val newState = initialState.update(dataA).update(dataB).update(dataC)
    newState.snapshot shouldBe Seq(dataA, dataB, dataC)
  }

  "Bulk get on category" should "get a correct data sequence" in {
    val newState = initialState.update(dataA).update(dataB).update(dataC)
    newState.get(group) shouldBe Seq(dataA, dataB, dataC)
    newState.get(emptyGroup).isEmpty shouldBe true
  }

}

object StateTest {
  val categoryA = LeafCategory("a", 100)
  val categoryB = LeafCategory("b", 100)
  val categoryC = LeafCategory("c", 100)
  val group = GroupCategory("abc", Set(categoryA, categoryB, categoryC))
  val emptyGroup = GroupCategory("_", Set.empty)
  val feeder = Sensor("temp")
  val dataA = Data("1", feeder, categoryA, 10, 1)
  val dataANewest = Data("1", feeder, categoryA, 11, 1)
  val dataB = Data("2", feeder, categoryB, 10, 1)
  val dataC = Data("3", feeder, categoryC, 10, 1)
}

