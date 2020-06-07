package it.unibo.client.demo.panels

import java.awt.{GridLayout, Label}

import it.unibo.client.demo.controller.DataSimulator
import javax.swing.{JButton, JPanel, JTextField}

class SimulationPanel(dataSimulator: DataSimulator) extends JPanel {

  val tickField = new JTextField("2000")
  val startSimulation = new JButton("Start simulation")
  val stopSimulation = new JButton("Stop simulation")

  setLayout(new GridLayout(3, 1))
  add(new Label("Tick"))
  add(tickField)
  add(startSimulation)
  add(stopSimulation)

  startSimulation.addActionListener(e => {
    dataSimulator.start(tickField.getText.toInt)
  })

  stopSimulation.addActionListener(e => {
    dataSimulator.stop()
  })
}
