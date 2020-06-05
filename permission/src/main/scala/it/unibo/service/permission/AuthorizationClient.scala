package it.unibo.service.permission
import java.net.URI

import io.vertx.core.buffer.Buffer
import io.vertx.lang.scala.VertxExecutionContext
import io.vertx.lang.scala.json.JsonObject
import io.vertx.scala.core.Vertx
import io.vertx.scala.ext.auth.PubSecKeyOptions
import io.vertx.scala.ext.auth.jwt.{JWTAuth, JWTAuthOptions}
import io.vertx.scala.ext.web.client.{HttpRequest, HttpResponse, WebClient, WebClientOptions}
import it.unibo.core.authentication.{AuthenticationParsers, SystemUser}
import it.unibo.core.client.{RestApiClient, RestClientServiceResponse}
import it.unibo.core.data.DataCategory
import it.unibo.core.microservice.{FutureService, Response}
import it.unibo.core.utils.HttpCode
import it.unibo.core.microservice.vertx._
import it.unibo.core.parser.DataParserRegistry

import scala.concurrent.Future

object AuthorizationClient {
  val READ = "/authorization/%s/read"
  val WRITE = "/authorization/%s/read"
}
class AuthorizationClient(uri : URI, dataParserRegistry: DataParserRegistry[JsonObject]) extends AuthorizationService with RestApiClient with RestClientServiceResponse {
  import AuthorizationClient._
  import UserMiddleware._

  private val stringPath = uri.toString
  private val vertx = Vertx.vertx()
  private val options = JWTAuthOptions().addPubSecKey(PubSecKeyOptions().setAlgorithm("HS256").setPublicKey("blabla").setSymmetric(true))

  private val provider = JWTAuth.create(vertx, options)
  private val clientOptions =  WebClientOptions()
    .setFollowRedirects(true)
    .setDefaultPort(uri.getPort)

  override val webClient: WebClient = WebClient.create(vertx, clientOptions)

  private implicit val executionContext: VertxExecutionContext = VertxExecutionContext(vertx.getOrCreateContext())

  override def authorizeRead(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val request = prepareWebClient(stringPath + READ.format(citizen), who)
      .addQueryParam("data_category", category.name).sendFuture()
    manageSingleCategoryResponse(request, category)
  }

  override def authorizeWrite(who: SystemUser, citizen: String, category: DataCategory): FutureService[DataCategory] = {
    val request = prepareWebClient(stringPath + WRITE.format(citizen), who)
      .addQueryParam("data_category", category.name).sendFuture()

    manageSingleCategoryResponse(request, category)
  }

  override def authorizedReadCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = {
    val request = prepareWebClient(stringPath + READ.format(citizen), who).sendFuture()
    manageMultipleCategoryResponse(request)
  }

  override def authorizedWriteCategories(who: SystemUser, citizen: String): FutureService[Seq[DataCategory]] = {
    val request = prepareWebClient(stringPath + READ.format(citizen), who).sendFuture()
    manageMultipleCategoryResponse(request)
  }

  private def prepareWebClient(path : String, who : SystemUser) : HttpRequest[Buffer] = {
    val jsonUser = AuthenticationParsers.SystemUserParser.encode(who)
    val token = provider.generateToken(jsonUser)
    webClient.get(path).putHeader(AUTHORIZATION_HEADER, token)
  }

  private def manageSingleCategoryResponse(future : Future[HttpResponse[Buffer]], category : DataCategory) : FutureService[DataCategory] = {
    parseServiceResponseWhenComplete(future) {
      case (HttpCode.Ok, _) => category
    }.toFutureService
  }
  //TODO gestisci meglio questa cosa
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
