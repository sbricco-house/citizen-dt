package it.unibo.core.utils

/**
 * Abstraction to generic error provided by a service
 */
sealed trait ServiceError {
  val message: String
}

object ServiceError {

  // Taxonomy of main Service Error (is not strictly related to HTTP error)

  sealed trait AuthError extends ServiceError
  sealed trait MissingError extends ServiceError

  case class Unauthenticated(message: String = "not authenticated") extends AuthError
  case class Unauthorized(message: String = "not authorized") extends AuthError

  case class MissingParameter(message: String = "missing or malformed required parameter") extends MissingError
  case class MissingResource(message: String = "resource not found") extends MissingError

}



