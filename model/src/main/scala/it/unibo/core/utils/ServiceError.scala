package it.unibo.core.utils

sealed trait ServiceError {
  val message: String
}

object ServiceError {

  sealed trait AuthError extends ServiceError
  sealed trait MissingError extends ServiceError

  case class Unauthenticated(message: String = "not authenticated") extends AuthError
  case class Unauthorized(message: String = "not authorized") extends AuthError

  case class MissingParameter(message: String = "missing required parameter") extends MissingError
  case class MissingResource(message: String = "not found") extends MissingError
}



