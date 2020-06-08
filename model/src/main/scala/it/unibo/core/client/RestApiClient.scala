package it.unibo.core.client

import io.vertx.scala.ext.web.client.WebClient

/**
 * Abstraction for a generic rest api client based on vertx webclient.
 * Like a client for [[it.unibo.core.microservice.vertx.RestApi]]
 */
trait RestApiClient {
  def webClient: WebClient
}