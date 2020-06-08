package it.unibo.client.demo

import javax.swing.SwingUtilities

import scala.concurrent.ExecutionContext

class SwingExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = SwingUtilities.invokeLater(runnable)
  override def reportFailure(cause: Throwable): Unit = throw cause
}
