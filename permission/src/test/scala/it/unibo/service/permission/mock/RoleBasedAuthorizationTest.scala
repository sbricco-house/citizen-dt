package it.unibo.service.permission.mock

import io.vertx.scala.core.Vertx
import it.unibo.core.authentication.{AuthenticationParsers, SystemUser, TokenIdentifier, VertxJWTProvider}
import it.unibo.core.data.GroupCategory
import it.unibo.core.microservice.{Fail, Response}
import it.unibo.service.permission.PermissionScope._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RoleBasedAuthorizationTest extends AnyFlatSpec with BeforeAndAfterAll with Matchers with ScalaFutures {
  implicit val ctx = scala.concurrent.ExecutionContext.global
  val vertx = Vertx.vertx()
  val authProvider = VertxJWTProvider.fromSymmetric("foo", vertx)
  def userFromIdentifer(id : String, role : String) : SystemUser = SystemUser("_", "_", "_", id, role)
  val sysUserA = userFromIdentifer("A", "citizen")
  val citizen = "A"
  val sysUserB = userFromIdentifer("B", "medical")
  val sysUserC = userFromIdentifer("C", "policeman")

  val tokenA = TokenIdentifier(authProvider.generateToken(AuthenticationParsers.SystemUserParser.encode(sysUserA)))
  val tokenB = TokenIdentifier(authProvider.generateToken(AuthenticationParsers.SystemUserParser.encode(sysUserB)))
  val tokenC = TokenIdentifier(authProvider.generateToken(AuthenticationParsers.SystemUserParser.encode(sysUserC)))

  val group = GroupCategory("G", Set(categoryA, categoryB))
  val allCategories = Seq(categoryA, categoryB, categoryC)

  val roleBased = MockRoleBasedAuthorization(
    authProvider = authProvider,
    allCategories = Set(categoryA, categoryB, categoryC),
    citizenWriteCategories = Set(categoryA, categoryB),
    readPermissionMap = Map("medical" -> Set(group), "policeman" -> Set(categoryA)),
    writePermissionMap = Map("medical" -> Set(categoryB), "policeman" -> Set())
  )

  "mock role based" should "allow all read operation to citizen" in {
    whenReady(roleBased.authorizedReadCategories(tokenA, citizen).future) {
      case Response(result) => allCategories shouldBe result
    }
    val (resultA, resultB, resultC) = (
      roleBased.authorizeRead(tokenA, citizen, categoryA).future,
      roleBased.authorizeRead(tokenA, citizen, categoryB).future,
      roleBased.authorizeRead(tokenA, citizen, categoryC).future
    )
    whenReady(resultA) {
      case Response(category) => categoryA shouldBe category
    }
    whenReady(resultB) {
      case Response(category) => categoryB shouldBe category
    }
    whenReady(resultC) {
      case Response(category) => categoryC shouldBe category
    }
  }

  "mock role based" should "allow write operation to citizen" in {
    whenReady(roleBased.authorizedWriteCategories(tokenA, citizen).future) {
      case Response(result) => Seq(categoryA, categoryB) shouldBe result
    }
    val (resultA, resultB, resultC) = (
      roleBased.authorizeWrite(tokenA, citizen, categoryA).future,
      roleBased.authorizeWrite(tokenA, citizen, categoryB).future,
      roleBased.authorizeWrite(tokenA, citizen, categoryC).future
    )
    whenReady(resultA) {
      case Response(category) => categoryA shouldBe category
    }
    whenReady(resultB) {
      case Response(category) => categoryB shouldBe category
    }
    whenReady(resultC) {
      case Response(category) => fail
      case Fail(error) => succeed
    }
  }

  "mock role based " should "allow other role to read citizen state" in {
    whenReady(roleBased.authorizedReadCategories(tokenB, citizen).future) {
      case Response(categories) => categories shouldBe Seq(group)
    }
    whenReady(roleBased.authorizedReadCategories(tokenC, citizen).future) {
      case Response(categories) => categories shouldBe Seq(categoryA)
    }

    whenReady(roleBased.authorizeRead(tokenB, citizen, categoryA).future) {
      case Response(category) => category shouldBe categoryA
    }

    whenReady(roleBased.authorizeRead(tokenB, citizen, categoryC).future) {
      case Response(category) => fail()
      case _ => succeed
    }
  }

  "mock role based " should "allow other role to write citizen state" in {
    whenReady(roleBased.authorizedWriteCategories(tokenB, citizen).future) {
      case Response(categories) => categories shouldBe Seq(categoryB)
    }
    whenReady(roleBased.authorizedWriteCategories(tokenC, citizen).future) {
      case Response(categories) => categories shouldBe Seq()
    }
  }
}
