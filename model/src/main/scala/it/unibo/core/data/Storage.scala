package it.unibo.core.data

import scala.util.{Failure, Try}

trait Storage[D, ID] {
  def store(data : D) : Try[Unit]
  def get(id : D) : Either[Option[D], Failure[Unit]]
  def find(policy : D => Boolean) : Either[Option[D], Failure[Unit]]
}
