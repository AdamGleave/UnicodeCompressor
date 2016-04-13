package uk.ac.cam.cl.arg58.mphil.utf8

import org.scalatest.FlatSpec

class TokenSpec extends FlatSpec  {
  "Tokens" should "be in bijection with the integers" in {
    for (i <- 0 to Token.Range - 1) {
      val t = Token.ofInt(i)
      assertResult(i, "token produced is [" + t + "]") {
        Token.toInt(t)
      }
    }
  }

  "Tokens" should "lie in the specified range" in {
    def check(i: Int): Unit = {
      assert(i < Token.Range)
      assert(i > 0)
    }

    val unicodeCharacters = (0 to 0x10ffff)
      .filter(cp => !UTF8.SurrogateCodePoints.contains(cp))
      .map(cp => UnicodeCharacter(cp))
    val illegalBytes = (0x80 to 0xff).map(b => IllegalByte(b.toByte))
    val surrogates = UTF8.SurrogateCodePoints.map(cp => SurrogateCodePoint(cp))
    val overlong =
      for ((range, index) <- UTF8.CodePoints.zipWithIndex;
            wrong_length <- (index + 2) to 4;
            cp <- range)
        yield Overlong(cp, wrong_length)
    val tooHigh = (0x110000 to 0x1fffff).map(cp => TooHigh(cp))
    val eof = Array(EOF())

    val allTokens = unicodeCharacters ++ illegalBytes ++ surrogates ++ overlong ++ tooHigh ++ eof
    for (t <- allTokens) {
      val c = Token.toInt(t)
      assert(c < Token.Range)
      assert(c >= 0)
    }
  }
}
