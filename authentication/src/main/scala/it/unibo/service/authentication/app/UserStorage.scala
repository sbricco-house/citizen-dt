package it.unibo.service.authentication.app

import it.unibo.core.authentication.SystemUser
import it.unibo.core.data.{InMemoryStorage, Storage}
import it.unibo.service.authentication.utils.Hash

object UserStorage {

  def generateDefault(): Storage[SystemUser, String] = {
    generate(Seq(
      5 -> "citizen",
      5 -> "stakeholder"
    ))
  }

  def generate(seeds: Seq[(Int, String)]): Storage[SystemUser, String] = {
    val userStorage: Storage[SystemUser, String] = InMemoryStorage[SystemUser, String]()
    seeds.flatMap(seed => generateSystemUsers(seed._1, seed._2)).foreach(u => userStorage.store(u.identifier, u))
    userStorage
  }

  private def generateSystemUsers(howMuch: Int, seedRole: String): Seq[SystemUser] = {
    (0 to howMuch).map(id => generateSystemUser(id, seedRole))
  }

  private def generateSystemUser(identifier: Int, seedRole: String): SystemUser = {
    val seedRoleId = s"$seedRole$identifier"
    SystemUser(s"$seedRoleId@email.com", s"$seedRoleId", Hash.SHA256.digest(s"$seedRoleId"), s"$identifier", s"$seedRole")
  }
}

