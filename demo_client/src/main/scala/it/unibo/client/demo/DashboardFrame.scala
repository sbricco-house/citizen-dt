package it.unibo.client.demo

import java.awt.{GridBagConstraints, GridBagLayout}

import io.vertx.core.json.JsonArray
import it.unibo.client.demo.panels.{DashboardState, SimulationPanel, StateManagementPanel}
import it.unibo.covid.data.Parsers
import it.unibo.service.citizen.client.CitizenClient
import javax.swing.JFrame

import scala.io.Source

class DashboardFrame(authUserProvider: AuthUserProvider, citizenId: String) extends JFrame {

  val executionContext = new SwingExecutionContext()
  val registry = Parsers.configureRegistryFromJson(new JsonArray(Source.fromResource("categories.json").mkString))
  val client = new CitizenClient(citizenId, registry)

  val controller = new CDTController(authUserProvider, client)

  setSize(500, 200)
  setLayout(new GridBagLayout())
  val constraint = new GridBagConstraints()
  constraint.fill = GridBagConstraints.HORIZONTAL
  constraint.gridx = 0
  constraint.gridy = 0
  constraint.weightx = 1
  add(new StateManagementPanel(controller, executionContext), constraint)

  constraint.gridx = 1
  constraint.gridy = 0
  constraint.weightx = 3
  add(new DashboardState(controller, executionContext), constraint)

  constraint.gridx = 2
  constraint.gridy = 0
  constraint.weightx = 1
  add(new SimulationPanel(controller), constraint)
}
