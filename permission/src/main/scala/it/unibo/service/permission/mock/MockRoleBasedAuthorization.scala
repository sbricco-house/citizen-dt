package it.unibo.service.permission.mock

import io.vertx.lang.scala.json.{JsonArray, JsonObject}
import io.vertx.scala.ext.auth.jwt.JWTAuth
import it.unibo.core.authentication.{SystemUser, TokenIdentifier, VertxJWTProvider}
import it.unibo.core.data.{DataCategory, DataCategoryOps, GroupCategory, LeafCategory}
import it.unibo.core.microservice.{Fail, FutureService, Response, ServiceResponse}
import it.unibo.core.parser.DataParserRegistry
import it.unibo.core.utils.ServiceError.Unauthorized
import it.unibo.service.permission.AuthorizationService
import it.unibo.core.microservice._
import it.unibo.core.microservice.vertx._
import it.unibo.core.authentication.VertxJWTProvider._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * more complex logic. In this case, the role of system user is used to verify the authenticity of the
 * system.
 */
object MockRoleBasedAuthorization {
  type Role = String
  type PermissionMap = Map[Role, Set[DataCategory]]

  /**
   * create a role based authorization using multiple permission map.
   * First of all, citizens has special authorization, they can read all their data. Some data category can't
   * be written (e.g. personal data). For this reason, a Set of write data category is passed.
   * For other stakeholder that want to access to a specific used, the permission follow two mop:
   *  readPermissionMap: tells if a specify role can read or not a set of cetegories
   *  writePermissionMap: tells if a specify role can write or not a set of cetegories
   * To access at Role, the service need to decode JWT token. For this reason a authProvider is
   * passed to decode tokens.
   * @param authProvider The provider used to decode jwt token
   * @param allCategories All categories supported by this authorization service
   * @param citizenWriteCategories All categories that a citizen can write
   * @param readPermissionMap The permission map that tells, for each role, what categories can be read from a citizen
   * @param writePermissionMap The permission map that tells, for each role, what categories can be write from a citizen
   * @param c The execution context used internally to manage the authorization request
   * @return the role based mock created.
   */
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
      case LeafCategory(name, _) => authorizeCategories.flatMap(DataCategoryOps.allChild).exists(_ == requestCategory)
    }

    private def getCategoriesFromOther(mapPermission : PermissionMap, who : SystemUser) : ServiceResponse[Seq[DataCategory]] = {
      mapPermission
        .get(who.role)
        .map(_.toSeq)
        .map(Response(_))
        .getOrElse(Fail(Unauthorized(s"User $who cannot access")))
    }

    override def toString: Role = s"RoleBased ${allCategories}, ${citizenWriteCategories}, ${readPermissionMap}, ${writePermissionMap}"
  }

  /**
   * create a role based authorization service from a json. The json must follow this structure :
   * {
   *  "read_map_permission" : [
   *    {
   *      "role" : "...", "categories" : ["..."]
   *    }
   *  ]
   *  "write_map_permission" : [
   *    {
   *      "role" : "...", "categories" : ["..."]
   *    }
   *  ],
   *  "categories" : ["..."],
   *  "write_citizen_permission" : ["..."]
   * }
   *
   * @param json The json the specify the authorization configuration
   * @param authProvider The provider used to decode jwt token
   * @param dataParserRegistry The parser used to decode the category name
   * @return the mock authorization service created.
   */
  def fromJson(json : JsonObject, authProvider : JWTAuth, dataParserRegistry: DataParserRegistry[_]) : Option[AuthorizationService] = {
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
