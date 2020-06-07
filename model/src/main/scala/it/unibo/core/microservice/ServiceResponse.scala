package it.unibo.core.microservice

/**
 * Abstraction to a generic response provided by a service.
 * Allow to manage in a simply manner the response in case of success or error without involving the exception.
 * @tparam T The content's type of response
 */
sealed trait ServiceResponse[+T] {
  def flatMap[U](f: T => ServiceResponse[U]): ServiceResponse[U]
  def map[U](f: T => U): ServiceResponse[U]
}

/**
 * Success response obtained by a service
 * @param content The content of response
 * @tparam T The content's type of response
 */
case class Response[+T](content: T) extends ServiceResponse[T] {
  override def flatMap[U](f: T => ServiceResponse[U]): ServiceResponse[U] = this match {
    case Response(v) => f(v)
  }
  override def map[U](f: T => U): ServiceResponse[U] = this match {
    case Response(v) => Response(f(v))
  }
}

/**
 * Fail response obtained by a service
 * @param error The error
 * @tparam T The error's type
 */
case class Fail[+T](error: T) extends ServiceResponse[Nothing] {
  override def flatMap[U](f: Nothing => ServiceResponse[U]): ServiceResponse[U] = this.asInstanceOf[Fail[T]]
  override def map[U](f: Nothing => U): ServiceResponse[U] = this.asInstanceOf[Fail[T]]
}