package it.unibo.core.microservice.coap

import org.eclipse.californium.core.coap.MediaTypeRegistry

sealed trait ContentFormat {
  def code : Int
}
protected class BaseContentFormat(override val code : Int) extends ContentFormat
case object PlainFormat extends BaseContentFormat(MediaTypeRegistry.TEXT_PLAIN)
case object JsonFormat extends BaseContentFormat(MediaTypeRegistry.APPLICATION_JSON)

