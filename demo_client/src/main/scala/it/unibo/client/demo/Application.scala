package it.unibo.client.demo

import java.net.URI

import it.unibo.client.demo.frame.LoginFrame
import it.unibo.service.authentication.client.AuthenticationClient

object Application extends App {
  val context = new SwingExecutionContext()
  val authenticationClient = AuthenticationClient(URI.create("http://localhost:8081"))

  val loginFrame = new LoginFrame(authenticationClient, context)
  loginFrame.setSize(500, 200)
  loginFrame.setVisible(true)
}
