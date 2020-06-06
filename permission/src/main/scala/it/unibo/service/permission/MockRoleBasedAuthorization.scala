package it.unibo.service.permission

import io.vertx.lang.scala.json.{JsonArray, JsonObject}
import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.authentication.{SystemUser, TokenIdentifier}
import it.unibo.core.data.{DataCategory, DataCategoryOps, GroupCategory, LeafCategory}
import it.unibo.core.microservice.FutureService
import it.unibo.core.authentication.VertxJWTProvider._
import it.unibo.core.microservice._
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.core.microservice.vertx._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
object MockRoleBasedAuthorization {
  type Role = String
  type PermissionMap = Map[Role, Set[DataCategory]]

  def apply(authProvider : JWTAuth, allCategories: Set[DataCategory], citizenWriteCategories : Set[DataCategory], readPermissionMap : PermissionMap, writePermissionMap : PermissionMap)(implicit c : ExecutionContext) : AuthorizationService = new AuthorizationService {
    def unauthorized(who : String) = Fail(Unauthorized(s"User $who cannot access to"))
    override def authorizeRead(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = {
      checkPermission(who, citizen, _ => Response(category), user => checkRWOther(readPermissionMap, user, category))
    }

    override def authorizeWrite(who: TokenIdentifier, citizen: String, category: DataCategory): FutureService[DataCategory] = {
      checkPermission(who,
        citizen,
        _ => citizenWriteCategories.find(_ == category)
          .map(Response(_))
          .getOrElse(unauthorized(citizen)),
        user => checkRWOther(writePermissionMap, user, category))
    }

    override def authorizedReadCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = {
      checkPermission(who, citizen, _ => Response(allCategories.toSeq), user => getCategoriesFromOther(readPermissionMap, user))
    }

    override def authorizedWriteCategories(who: TokenIdentifier, citizen: String): FutureService[Seq[DataCategory]] = {
      checkPermission(who, citizen, _ => Response(citizenWriteCategories.toSeq), user => getCategoriesFromOther(writePermissionMap, user))
    }

    private def checkPermission[A](who : TokenIdentifier, citizen : String, ifCitizen : SystemUser => ServiceResponse[A], ifOther: SystemUser => ServiceResponse[A]) : FutureService[A] = {
      authProvider.extractSystemUser(who).transformToFutureService {
        case Success(user) => user.identifier match {
          case `citizen` => ifCitizen(user)
          case _ => ifOther(user)
        }
        case Failure(exception) => Fail(exception)
      }
    }

    private def checkRWOther(mapPermission : Map[String, Set[DataCategory]], who : SystemUser, category : DataCategory) : ServiceResponse[DataCategory] = {
      mapPermission.get(who.role).filter(isOperationEnabled(_, category)) match {
        case None => Fail(Unauthorized(s"User $who cannot access to"))
        case Some(_) => Response(category)
      }
    }

    private def isOperationEnabled(authorizeCategories: Set[DataCategory], requestCategory: DataCategory): Boolean = requestCategory match {
      case GroupCategory(name, _) => authorizeCategories.exists(_.name == name)
      case LeafCategory(name, _) => DataCategoryOps.allChild(requestCategory).exists(leaf => leaf.name == name)
    }

    private def getCategoriesFromOther(mapPermission : PermissionMap, who : SystemUser) : ServiceResponse[Seq[DataCategory]] = {
      mapPermission
        .get(who.role)
        .map(_.toSeq)
        .map(Response(_))
        .getOrElse(Fail(Unauthorized(s"User $who cannot access")))
    }
  }

  def fromJson(authProvider : JWTAuth, json : JsonObject, dataParserRegistry: DataParserRegistry[_]) : Option[AuthorizationService] = {
    implicit val ctx = scala.concurrent.ExecutionContext.global
    def decodeCategories(categories : Seq[String]) : Set[DataCategory] = categories
      .map(dataParserRegistry.decodeCategory)
      .collect { case Some(category) => category }
      .toSet

    def createPermissionMap(json : JsonArray) : Option[PermissionMap] = json.getAsObjectSeq match {
      case Some(seq) =>
        val permissionMap = seq
          .map(elem => (elem.getAsString("role"), elem.getAsArray("categories")))
          .collect { case (Some(role), Some(categories)) => (role, categories)}
          .map { case (role, categories) => (role, categories.getAsStringSeq)}
          .collect { case (role, Some(stringCategories)) => (role, stringCategories)}
          .map { case (role, categories) => (role, decodeCategories(categories)) }
          .toMap
        Some(permissionMap)
      case other => None
    }

    for {
      readMap <- json.getAsArray("read_map_permission")
      readMapFetched <- createPermissionMap(readMap)
      writeMap <- json.getAsArray("write_map_permission")
      writeMapFetched <- createPermissionMap(writeMap)
      allCategories <- json.getAsArray("categories")
      allCategoriesFetched <- allCategories.getAsStringSeq
      writeCitizen <- json.getAsArray("write_citizen_permission")
      writeCitizenFetched <- writeCitizen.getAsStringSeq
    } yield MockRoleBasedAuthorization(authProvider, decodeCategories(allCategoriesFetched), decodeCategories(writeCitizenFetched), readMapFetched, writeMapFetched)
  }
}
