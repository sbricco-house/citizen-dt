package it.unibo.client.demo.controller

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import it.unibo.core.data.{Data, Feeder, LeafCategory, Sensor}
import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

object DataSimulator {
  private def generateBetween(min: Double, max: Double): Double = (Math.random * (max - min + 1)) + min
  private val categories = Seq("spo2")
  private val feederSim = (category: String) => Sensor("miband4")
  private val valueSim = (category: String) => category match {
    case "spo2" => generateBetween(30, 50)
  }

  def apply() = new DataSimulator(categories, valueSim, feederSim)
}

class DataSimulator(categories: Seq[String], simulationStrategy: String => Any, feederStrategy: String => Feeder) {

  private val _simulatedData = PublishSubject[Data]()
  val simulatedData: Observable[Data] = _simulatedData

  private val scheduler = Executors.newScheduledThreadPool(1)
  private var task: ScheduledFuture[_] = _

  def start(tick: Int): Unit = {
    task = scheduler.scheduleAtFixedRate(onTick, tick, tick, TimeUnit.MILLISECONDS)
  }

  def stop(): Unit = {
    task.cancel(true)
  }

  private val onTick = new Runnable {
    override def run(): Unit = {
      categories.map(generateSingleData).foreach {
        _simulatedData.onNext
      }
    }
  }

  private def generateSingleData(categoryName: String): Data = {
    val timestamp = System.currentTimeMillis()
    val value = simulationStrategy(categoryName)
    val feeder = feederStrategy(categoryName)
    Data("", feeder, LeafCategory(categoryName, -1), timestamp, value)
  }
}

