package uk.ac.cam.cl.arg58.mphil.utf8

import org.scalatest.FlatSpec

class TokenSpec extends FlatSpec  {
  "A token" should "have an invertible conversion to/from integers" in {
    for (i <- 0 to Token.Range - 1) {
      val t = Token.ofInt(i)
      assertResult(i, "token produced is [" + t + "]") {
        Token.toInt(t)
      }
    }
  }
}
