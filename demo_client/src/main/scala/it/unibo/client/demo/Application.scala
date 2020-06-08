package it.unibo.client.demo

import java.net.URI

import it.unibo.client.demo.frame.LoginFrame
import it.unibo.service.authentication.client.AuthenticationClient

object Application extends App {git
  val context = new SwingExecutionContext()
  val authenticationClient = AuthenticationClient(URI.create("http://localhost:8081"))

  // TODO: improve that, login frame is bad name
  val loginFrame = new LoginFrame(authenticationClient, context)
  loginFrame.setSize(500, 200)
  loginFrame.setVisible(true)

  val other = new LoginFrame(authenticationClient, context)
  other.setSize(500, 200)
  other.setVisible(true)

}
