package it.unibo.core.microservice

sealed trait ServiceResponse[+T] {
  def flatMap[U](f: T => ServiceResponse[U]): ServiceResponse[U]
  def map[U](f: T => U): ServiceResponse[U]
}
case class Response[+T](content: T) extends ServiceResponse[T] {
  override def flatMap[U](f: T => ServiceResponse[U]): ServiceResponse[U] = this match {
    case Response(v) => f(v)
  }
  override def map[U](f: T => U): ServiceResponse[U] = this match {
    case Response(v) => Response(f(v))
  }
}

case class Fail[+T](error: T) extends ServiceResponse[Nothing] {
  override def flatMap[U](f: Nothing => ServiceResponse[U]): ServiceResponse[U] = this.asInstanceOf[Fail[T]]
  override def map[U](f: Nothing => U): ServiceResponse[U] = this.asInstanceOf[Fail[T]]
}