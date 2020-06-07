package it.unibo.core.microservice

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
//TODO add documentation
object FutureService {
  def apply[A](response: ServiceResponse[A]): FutureService[A] = Future.successful(response).toFutureService
  def response[A](content: A): FutureService[A] = apply(Response(content))
  def fail[A](error: A): FutureService[Nothing] = apply(Fail(error))
}

case class FutureService[+A](future: Future[ServiceResponse[A]]) {
  def flatMap[U](f: A => FutureService[U])(implicit executionContext: ExecutionContext): FutureService[U] =
    FutureService(this.future.flatMap {
      case Response(content) => f(content).future
      case Fail(error) => FutureService.fail(error).future
    })

  def map[U](f: A => U)(implicit executionContext: ExecutionContext): FutureService[U] = {
    FutureService(this.future.map(_.map(f)))
  }

  def whenComplete[U](f: ServiceResponse[A] => U)(implicit executionContext: ExecutionContext): Unit = {
    future.onComplete {
      case Success(value) => f(value)
      case Failure(exception) => f(Fail(exception))
    }
  }
}