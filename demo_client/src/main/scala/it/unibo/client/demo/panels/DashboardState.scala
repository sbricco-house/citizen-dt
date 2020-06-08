package it.unibo.client.demo.panels

import java.awt.{Dimension, GridLayout}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

import it.unibo.client.demo.SwingExecutionContext
import it.unibo.client.demo.controller.CDTController
import it.unibo.core.data.{Data, Resource, Sensor}
import javax.swing.table.DefaultTableModel
import javax.swing.{DefaultListModel, JList, JPanel, JScrollPane, JTable}
import monix.execution.Scheduler

import scala.collection.JavaConverters._
class DashboardState(controller: CDTController, implicit val swingExecutionContext: SwingExecutionContext) extends JPanel {
  this.setMinimumSize(new Dimension(200,200))
  val scheduler = Scheduler(swingExecutionContext)

  val tableModel = new DefaultTableModel()
  Seq("id", "category", "feeder", "date", "value").foreach{tableModel.addColumn(_)}
  val table = new JTable(tableModel)
  val tablePane = new JScrollPane(table)
  setLayout(new GridLayout(1, 1))
  add(new JScrollPane(table))
  controller.state.observeOn(scheduler).foreach { data =>
    println("Swing" + Thread.currentThread().getName) // TODO: remove it
    for(i <- 0 until tableModel.getRowCount) {
      tableModel.removeRow(0)
    }
    data.map(toTableFormat).foreach(tableModel.addRow)
  }(scheduler)

  private def toTableFormat(data : Data) : Array[AnyRef] = {
    val feeder = data.feeder match {
      case Resource(uri) => uri
      case Sensor(name) => name
    }
    val date = timestampToDate(data.timestamp)
    Seq(data.identifier, data.category.name, feeder, date, data.value.toString).asJava.toArray()
  }

  private val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
  private def timestampToDate(timestamp: Long): String = {
    val time = new Timestamp(timestamp)
    dateFormatter.format(new Date(time.getTime))
  }
}
