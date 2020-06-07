package it.unibo.client.demo.panels

import java.awt.GridLayout
import java.awt.event.{ActionEvent, ActionListener}

import it.unibo.client.demo.{CDTController, SwingExecutionContext}
import it.unibo.core.microservice.{Fail, Response}
import javax.swing.{JButton, JOptionPane, JPanel}

class StateManagementPanel(controller: CDTController, implicit val swingExecutionContext: SwingExecutionContext) extends JPanel {

  val refreshState = new JButton("Refresh state")
  val createPhysicalLink = new JButton("Create physical link")
  val observeCitizen = new JButton("Observe citizen")

  setLayout(new GridLayout(2, 1))
  add(refreshState)
  add(if (controller.canObserveCitizen) observeCitizen else  createPhysicalLink)

  createPhysicalLink.addActionListener(e => {
    controller.createLink().whenComplete {
      case Response(_) => JOptionPane.showMessageDialog(null, "Link successfully created")
      case Fail(error) => JOptionPane.showMessageDialog(null, s"Link error: ${error}")
    }
  })

  refreshState.addActionListener((e: ActionEvent) => {
    controller.fetchState()
  })

}
