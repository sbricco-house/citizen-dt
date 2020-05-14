package it.unibo.service.citizen.utils

import scala.concurrent.{ExecutionContext, Future}

case class FutureOption[+A](future: Future[Option[A]]) {
  def flatMap[B](f: A => FutureOption[B])(implicit executionContext: ExecutionContext): FutureOption[B] = {
    FutureOption(future.flatMap {
      case Some(value) => f(value).future
      case None => Future.successful(None)
    })
  }
  def map[B](f: A => B)(implicit executionContext: ExecutionContext): FutureOption[B] = {
    FutureOption(future.map(_.map(f)))
  }
}

