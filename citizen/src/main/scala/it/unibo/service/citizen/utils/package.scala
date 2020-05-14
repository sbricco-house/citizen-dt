package it.unibo.service.citizen

import scala.concurrent.Future

package object utils {
  implicit class FutureOptionConversion[+B](future: Future[Option[B]]) {
    def toFutureOption: FutureOption[B] = FutureOption(future)
  }
}
