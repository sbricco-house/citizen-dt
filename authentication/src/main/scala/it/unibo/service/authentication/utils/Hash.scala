package it.unibo.service.authentication.utils

import java.math.BigInteger
import java.security.MessageDigest

trait Hash {
  def digest(input: String) : String
}

object Hash {
  object SHA256 extends Hash {
    private val hash = MessageDigest.getInstance("SHA-256")

    override def digest(input: String): String = {
      val digest = hash.digest(input.getBytes("UTF-8"))
      String.format("%064x", new BigInteger(1, digest))
    }
  }
}
