package it.unibo.client.demo

import java.awt.GridLayout

import it.unibo.core.authentication.Resources.AuthenticationInfo
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.service.authentication.client.AuthenticationClient
import javax.swing._

class LoginFrame(authenticationClient: AuthenticationClient, implicit val swingExecutionContext: SwingExecutionContext) extends JFrame {

  val emailField = new JTextField()
  val passwordField = new JTextField()
  val loginButton = new JButton("Login")

  setLayout(new GridLayout(3, 2))

  add(new JLabel("Email"))
  add(emailField)
  add(new JLabel("Password"))
  add(passwordField)
  add(loginButton)

  loginButton.addActionListener(e => {
    if(!emailField.getText.isEmpty && !passwordField.getText.isEmpty) {
      authenticationClient.login(emailField.getText, passwordField.getText).whenComplete {
        case Response(content) => goToDashboard(content)
        case Fail(error) => JOptionPane.showMessageDialog(this, "Error during login: " + error)
      }
    }
  })

  private def promptCitizenSelection(): String = JOptionPane.showInputDialog(this, "Insert the citizen id to observe")

  private def goToDashboard(authInfo: AuthenticationInfo): Unit = {
    val provider = new AuthUserProvider {
      override def currentToken(): TokenIdentifier = TokenIdentifier(authInfo.token.token)
      override def currentUser(): SystemUser = authInfo.user
    }

    val citizenId = if(authInfo.user.role == "citizen") authInfo.user.identifier else promptCitizenSelection()

    val dashboard = new DashboardFrame(provider, citizenId)
    dashboard.setVisible(true)
    setVisible(false)
  }
}
