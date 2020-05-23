package it.unibo.core.client

import io.vertx.scala.ext.web.client.WebClient

trait RestApiClient {
  def webClient: WebClient
}