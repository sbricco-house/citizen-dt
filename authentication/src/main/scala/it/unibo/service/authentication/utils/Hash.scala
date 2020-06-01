package it.unibo.service.authentication.utils

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Abstraction to a generic Hash function.
 */
trait Hash {
  /**
   * Apply an hash function to an input and produce a digest.
   * @param input Input string where to apply the hash function
   * @return The digest of input string
   */
  def digest(input: String) : String
}

object Hash {
  /**
   * Hash function using SHA-256 algorithm.
   */
  object SHA256 extends Hash {
    private val hash = MessageDigest.getInstance("SHA-256")

    override def digest(input: String): String = {
      val digest = hash.digest(input.getBytes("UTF-8"))
      String.format("%064x", new BigInteger(1, digest))
    }
  }
}
