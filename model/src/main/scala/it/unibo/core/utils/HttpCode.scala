package it.unibo.core.utils

/**
 * Abstraction to HTTP response codes
 */
sealed trait HttpCode {
  def code: Int
}

object HttpCode {
  /**
   * Return the right HttpCode starting from the numeric representation.
   * @param code Numeric representation of http code.
   * @return An HttpCode
   */
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

  protected sealed abstract class BaseCode(override val code : Int) extends HttpCode
  sealed trait Success extends HttpCode
  sealed trait Error extends HttpCode
  sealed trait ClientError extends Error
  sealed trait ServerError extends Error

  case object Ok extends BaseCode(200) with Success
  case object Created extends BaseCode(201) with Success
  case object NoContent extends BaseCode(204) with Success

  case object BadRequest extends BaseCode(400) with ClientError
  case object Unauthorized extends BaseCode(401) with ClientError
  case object Forbidden extends BaseCode(403) with ClientError
  case object NotFound extends BaseCode(404) with ClientError
  case object Conflict extends BaseCode(409) with ClientError

  case object InternalError extends BaseCode(500) with ServerError
}
