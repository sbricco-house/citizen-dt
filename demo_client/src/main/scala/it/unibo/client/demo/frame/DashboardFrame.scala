package it.unibo.client.demo.frame

import java.awt.{GridBagConstraints, GridBagLayout}
import java.net.URI

import io.vertx.core.json.JsonArray
import it.unibo.client.demo.controller.{CDTController, DataSimulator}
import it.unibo.client.demo.panels.{DashboardState, SimulationPanel, StateManagementPanel}
import it.unibo.client.demo.{AuthUserProvider, SwingExecutionContext}
import it.unibo.covid.data.{Categories, Parsers}
import it.unibo.service.citizen.client.CitizenClient
import it.unibo.service.permission.client.AuthorizationClient
import javax.swing.JFrame

import scala.io.Source
import it.unibo.client.demo.ServicePorts._
import it.unibo.core.data.DataCategoryOps
import it.unibo.core.microservice.Response
class DashboardFrame(authUserProvider: AuthUserProvider, citizenId: String) extends JFrame {
  this.setTitle(s"$citizenId observing Dashboard")

  val executionContext = new SwingExecutionContext()
  val registry = Parsers.configureRegistryFromJson(new JsonArray(Source.fromResource("categories.json").mkString))
  val client = new CitizenClient(citizenId, registry)
  val authorizationClient = AuthorizationClient(URI.create(s"http://localhost:$authorizationPort"), registry)

  val dataSimulator = DataSimulator()
  val controller = new CDTController(dataSimulator, authUserProvider, client)

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
  setVisible(false)
  authorizationClient.authorizedWriteCategories(authUserProvider.currentToken(), citizenId).whenComplete {
    case Response(content) =>
      val flattenCategories = content.flatMap(DataCategoryOps.allChild)
      add(new SimulationPanel(dataSimulator, flattenCategories), constraint)
      setVisible(true)
      this.pack()
    case _ =>
      val standardCategories = DataCategoryOps.allChild(Categories.medicalDataCategory) + Categories.positionCategory
      add(new SimulationPanel(dataSimulator, standardCategories.toSeq), constraint)
      setVisible(true)
      this.pack()
  }(scala.concurrent.ExecutionContext.global)

}
