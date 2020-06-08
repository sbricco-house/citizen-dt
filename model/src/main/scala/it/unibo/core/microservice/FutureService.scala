package it.unibo.core.microservice

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Abstraction of pending service's response [ServiceResponse].
 * Its a wrap of scala futures that manage a ServiceResponse in case of success or failure.
 * Allow to handle the failure of service in a different manner compared to scala futures. With FutureService
 * we can distinguish two type of failure:
 * - a logic failure, represented by [Fail]: when the service send a response that represent a Fail.
 * - a connection failure, represented by Exception: when the service is not available or there is problem over the connection (e.g. tcp/ip).
 */
object FutureService {
  /**
   * Create a future service from a Service Response
   * @param response ServiceResponse
   * @tparam A The content's type of response
   * @return A future service of response
   */
  def apply[A](response: ServiceResponse[A]): FutureService[A] = Future.successful(response).toFutureService

  /**
   * Create a success FutureService response.
   * @param content the content of Response
   * @tparam A The content's type of response
   * @return A future service with success response
   */
  def response[A](content: A): FutureService[A] = apply(Response(content))

  /**
   * Create a failed FutureService Response.
   * @param error The error.
   * @tparam A The type of error
   * @return A future service with fail response
   */
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