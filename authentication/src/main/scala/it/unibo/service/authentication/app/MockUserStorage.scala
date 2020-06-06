package it.unibo.service.authentication.app

import io.vertx.lang.scala.json.{JsonArray, JsonObject}
import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.{Data, InMemoryStorage, Storage}
import it.unibo.core.parser.ParserLike
import it.unibo.core.parser.ParserLike.Parser
import it.unibo.service.authentication.utils.Hash
import it.unibo.core.microservice.vertx._

/**
 * A simple object for mock the user storage.
 */
object MockUserStorage {
  
  /**
   * Generate a default user storage composed by 5 citizen and 5 stakeholder.
   * @return A storage of SystemUser using email as key.
   * @example An example of System user. email: citizen3@email.com and password: citizen3
   */
  def generateDefault(): Storage[SystemUser, String] = {
    generate(Seq(
      5 -> "citizen",
      5 -> "stakeholder"
    ))
  }

  /**
   * Generate a user storage starting from a set of seed.
   * @param seeds Set of seed. Each seed is pair of: - number of member, - role of each member
   *              e.g. 5 -> "citizen" implies 5 citizen.
   * @return A storage of System user using email as key.
   * @example An example of System user. email: citizen3@email.com and password: citizen3
   */
  def generate(seeds: Seq[(Int, String)]): Storage[SystemUser, String] = {
    val userStorage: Storage[SystemUser, String] = InMemoryStorage[SystemUser, String]()
    seeds.flatMap(seed => generateSystemUsers(seed._1, seed._2)).foreach(u => userStorage.store(u.email, u))
    userStorage
  }

  private def generateSystemUsers(howMuch: Int, seedRole: String): Seq[SystemUser] = {
    (0 to howMuch).map(id => generateSystemUser(id, seedRole))
  }

  private def generateSystemUser(identifier: Int, seedRole: String): SystemUser = {
    val seedRoleId = s"$seedRole$identifier"
    SystemUser(s"$seedRoleId@email.com", s"$seedRoleId", Hash.SHA256.digest(s"$seedRoleId"), s"$seedRoleId", s"$seedRole")
  }

  val userStorageParser = {
    def userFromJson(json : JsonObject) : Option[SystemUser] = for {
      id <- json.getAsString("identifier")
      user <- json.getAsString("username")
      email <- json.getAsString("email")
      pass <- json.getAsString("password")
      role <- json.getAsString("role")
    } yield SystemUser(email, user, pass, id, role)

    ParserLike.decodeOnly[JsonArray, Storage[SystemUser, String]](
      decodeFunction = json => json.getAsObjectSeq match {
        case Some(users) =>
          val storage =  InMemoryStorage[SystemUser, String]
          users
            .map(userFromJson)
            .collect { case Some(user) => user }
            .foreach { case user => storage.store(user.identifier, user)}
          Some(storage)
        case other => None
      }
    )
  }
}

