package it.unibo.core

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

package object microservice {
  implicit class FutureConverter[+A](future: Future[ServiceResponse[A]]) {
    def toFutureService: FutureService[A] = FutureService(future)
  }

  implicit class RichFuture[+A](future: Future[A]) {
    def transformToFutureService[S](f: Try[A] => ServiceResponse[S])(implicit execution: ExecutionContext): FutureService[S] = {
      future.transform(t => Success(f(t))).toFutureService
    }
  }
}
