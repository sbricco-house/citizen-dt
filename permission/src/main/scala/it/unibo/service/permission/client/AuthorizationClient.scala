package it.unibo.service.permission.client

import java.net.URI

import io.vertx.core.buffer.Buffer
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse, WebClient, WebClientOptions}
import it.unibo.core.authentication.TokenIdentifier
import it.unibo.core.authentication.middleware.UserMiddleware
import it.unibo.core.client.{RestApiClient, RestClientServiceResponse}
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.FutureService
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.HttpCode
import it.unibo.service.permission.AuthorizationService

import scala.concurrent.Future

object AuthorizationClient {
  val READ = "/authorization/%s/read"
  val WRITE = "/authorization/%s/write"

  def apply(uri : URI, dataParserRegistry: DataParserRegistry[JsonObject]) : AuthorizationClient = new AuthorizationClient(uri, dataParserRegistry)
}

/**
 * The client that communicate with an authorization service.
 * @param uri The uri in which the microservice is hosted
 * @param dataParserRegistry The parser used to marshall and unmarshall information
 */
class AuthorizationClient(uri : URI, dataParserRegistry: DataParserRegistry[JsonObject]) extends AuthorizationService with RestApiClient with RestClientServiceResponse {
  import AuthorizationClient._
  import it.unibo.core.authentication.middleware.UserMiddleware._

  private val stringPath = uri.toString
  private val vertx = Vertx.vertx()
  private val clientOptions =  WebClientOptions()
    .setFollowRedirects(true)
    .setDefaultPort(uri.getPort)

  override val webClient: WebClient = WebClient.create(vertx, clientOptions)
  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val request = prepareWebClient(stringPath + READ.format(citizen), who)
      .addQueryParam("data_category", category.name).sendFuture()
    manageSingleCategoryResponse(request, category)
  }

  override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val request = prepareWebClient(stringPath + WRITE.format(citizen), who)
      .addQueryParam("data_category", category.name).sendFuture()
    manageSingleCategoryResponse(request, category)
  }

  override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = {
    val request = prepareWebClient(stringPath + READ.format(citizen), who).sendFuture()
    manageMultipleCategoryResponse(request)
  }

  override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = {
    val request = prepareWebClient(stringPath + WRITE.format(citizen), who).sendFuture()
    manageMultipleCategoryResponse(request)
  }

  private def prepareWebClient(path : String, who : TokenIdentifier) : HttpRequest[Buffer] = {
    webClient.get(path).putHeader(AUTHORIZATION_HEADER, who.bearer)
  }

  private def manageSingleCategoryResponse(future : Future[HttpResponse[Buffer]], category : DataCategory) : FutureService[DataCategory] = {
    parseServiceResponseWhenComplete(future) {
      case (HttpCode.Ok, _) => category
    }.toFutureService
  }
  //TODO this method is not clear. clarify it.
  private def manageMultipleCategoryResponse(future : Future[HttpResponse[Buffer]]) : FutureService[Seq[DataCategory]] = {
    parseServiceResponseWhenComplete[Buffer, Seq[DataCategory]](future) {
      case (HttpCode.Ok, response) =>
        val sequenceString = for {
          array <- JsonConversion.arrayFromString(response)
          sequence <- array.getAsStringSeq
        } yield sequence
        sequenceString match {
          case Some(sequence) => sequence
            .map(dataParserRegistry.decodeCategory)
            .collect { case Some(data) => data }
          case other => Seq.empty
        }
    }.toFutureService
  }
}
