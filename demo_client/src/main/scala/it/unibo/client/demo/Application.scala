package it.unibo.client.demo

import java.net.URI

import it.unibo.client.demo.frame.LoginFrame
import it.unibo.service.authentication.client.AuthenticationClient
import ServicePorts._

object Application extends App {
  val context = new SwingExecutionContext()
  val authenticationClient = AuthenticationClient(URI.create(s"http://localhost:$authenticationPort"))

  val loginFrame = new LoginFrame(authenticationClient, context)
  loginFrame.setSize(500, 200)
  loginFrame.setVisible(true)
  new LoginFrame(authenticationClient, context).setVisible(true)

  new LoginFrame(authenticationClient, context).setVisible(true)
}
