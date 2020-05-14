package it.unibo.core

import scala.concurrent.Future

package object microservice {
  implicit class FutureConverter[+A](future: Future[ServiceResponse[A]]) {
    def toFutureService: FutureService[A] = FutureService(future)
  }
}
