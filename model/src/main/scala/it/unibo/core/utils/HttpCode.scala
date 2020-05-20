package it.unibo.core.utils

sealed trait HttpCode {
  def code: Int
}

object HttpCode {

  def apply(code: Int): HttpCode = code match {
    case 200 => Ok
    case 201 => Created
    case 204 => NoContent
    case 400 => BadRequest
    case 401 => Unauthorized
    case 403 => Forbidden
    case 404 => NotFound
    case _ => InternalError
  }

  sealed trait Success extends HttpCode
  sealed trait Error extends HttpCode
  sealed trait ClientError extends Error
  sealed trait ServerError extends Error

  case object Ok extends Success {
    override def code: Int = 200
  }

  case object Created extends Success {
    override def code: Int = 201
  }

  case object NoContent extends Success {
    override def code: Int = 204
  }

  case object BadRequest extends ClientError {
    override def code: Int = 400
  }

  case object Unauthorized extends ClientError {
    override def code: Int = 401
  }

  case object Forbidden extends ClientError {
    override def code: Int = 403
  }

  case object NotFound extends ClientError {
    override def code: Int = 404
  }

  case object Conflict extends ClientError {
    override def code: Int = 409
  }

  case object InternalError extends ServerError {
    override def code: Int = 500
  }
}
