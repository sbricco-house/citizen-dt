package it.unibo.client.demo.panels

import java.awt.{GridLayout, Label}

import it.unibo.client.demo.controller.DataSimulator
import it.unibo.core.data.DataCategoryOps
import it.unibo.covid.data.Categories
import javax.swing.{JButton, JCheckBox, JPanel, JTextField}

class SimulationPanel(dataSimulator: DataSimulator) extends JPanel {

  val tickField = new JTextField("2000")
  val startSimulation = new JButton("Start simulation")
  val stopSimulation = new JButton("Stop simulation")

  setLayout(new GridLayout(3, 1))
  add(new Label("Tick"))
  add(tickField)
  add(startSimulation)
  add(stopSimulation)
  val categories = DataCategoryOps.allChild(Categories.medicalDataCategory) + Categories.positionCategory

  val jboxList = createJboxList(categories.map(_.name).toSeq)
  jboxList.foreach(box => {
    box.setBounds(0,0,100,20)
    add(box)
  })

  private def createJboxList(name : Seq[String]) : Seq[JCheckBox] = {
    name.map(new JCheckBox(_, true))
  }
  startSimulation.addActionListener(e => {
    val selectedCategories = jboxList.filter(_.isSelected).map(_.getText)
    if(selectedCategories.nonEmpty) {
      dataSimulator.categories = selectedCategories
    }
    dataSimulator.start(tickField.getText.toInt)
  })

  stopSimulation.addActionListener(e => {
    dataSimulator.stop()
  })
}
