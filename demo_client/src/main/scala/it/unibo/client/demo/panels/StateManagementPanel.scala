package it.unibo.client.demo.panels

import java.awt.GridLayout
import java.awt.event.ActionEvent

import it.unibo.client.demo.SwingExecutionContext
import it.unibo.client.demo.controller.CDTController
import it.unibo.core.microservice.{Fail, Response}
import javax.swing._

class StateManagementPanel(controller: CDTController, implicit val swingExecutionContext: SwingExecutionContext) extends JPanel {

  val refreshState = new JButton("Refresh state")
  val createPhysicalLink = new JButton("Create physical link")
  val observeCitizen = new JButton("Observe citizen")
  val observeCategoryLabel = new JLabel("Category to observe")
  val observeCategory = new JTextField()

  if(controller.canObserveCitizen) {
    setLayout(new GridLayout(4, 1))
    add(observeCategoryLabel)
    add(observeCategory)
    add(observeCitizen)
  } else {
    setLayout(new GridLayout(2, 1))
    add(createPhysicalLink)
  }
  add(refreshState)

  createPhysicalLink.addActionListener(e => {
    controller.createLink().whenComplete {
      case Response(_) => JOptionPane.showMessageDialog(null, "Link successfully created")
      case Fail(error) => JOptionPane.showMessageDialog(null, s"Link error: ${error}")
    }
  })

  observeCitizen.addActionListener(e => {
    if(!observeCategory.getText.isBlank) {
      controller.observe(observeCategory.getText).whenComplete {
        case Response(_) => JOptionPane.showMessageDialog(null, s"Observing ${observeCategoryLabel.getText}")
        case Fail(error) => JOptionPane.showMessageDialog(null, "Observe error: " + error)
      }
    } else {
      JOptionPane.showMessageDialog(null, "Category to observe is mandatory")
    }
  })

  refreshState.addActionListener((e: ActionEvent) => {
    controller.fetchState().whenComplete {
      case Response(_) => JOptionPane.showMessageDialog(null, "Fetched")
      case Fail(error) => JOptionPane.showMessageDialog(null, "Error during fetch: " + error)
    }
  })

}
