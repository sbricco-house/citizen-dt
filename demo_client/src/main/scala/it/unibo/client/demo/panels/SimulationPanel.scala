package it.unibo.client.demo.panels

import java.awt.{GridLayout, Label}

import it.unibo.client.demo.CDTController
import it.unibo.core.data.{Data, LeafCategory, Sensor}
import javax.swing.{JButton, JPanel, JTextField}

class SimulationPanel(controller: CDTController) extends JPanel {

  val tickField = new JTextField("2000")
  val startSimulation = new JButton("Start simulation")
  val stopSimulation = new JButton("Stop simulation")

  setLayout(new GridLayout(3, 1))
  add(new Label("Tick"))
  add(tickField)
  add(startSimulation)
  add(stopSimulation)



  startSimulation.addActionListener(e => {
    new Thread(() => {
      println("Started!")
      while (true) {
        println("NEw data sent")
        controller.updateState(Seq(Data("34534635", Sensor("mi_band_4"), LeafCategory("spo2", 10), 43, System.currentTimeMillis())))
        Thread.sleep(2000)
      }
    })  .start()
  })


  private def onSimulationTick(): Unit = {
  }

}
