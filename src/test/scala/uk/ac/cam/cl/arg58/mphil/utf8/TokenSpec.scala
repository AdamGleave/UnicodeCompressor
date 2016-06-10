/* Copyright (C) 2016, Adam Gleave

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
    val surrogates = UTF8.SurrogateCodePoints.map(cp => SurrogateCodePoint(cp))
    val overlong =
      for ((range, index) <- UTF8.CodePoints.zipWithIndex;
            wrong_length <- (index + 2) to 4;
            cp <- range)
        yield Overlong(cp, wrong_length)
    val tooHigh = (0x110000 to 0x1fffff).map(cp => TooHigh(cp))
    val illegalBytes = (0x80 to 0xff).map(b => IllegalByte(b.toByte))
    val eof = Array(EOF())

    val allTokens = unicodeCharacters ++ surrogates ++ overlong ++ tooHigh ++ illegalBytes ++ eof
    for (t <- allTokens) {
      val c = Token.toInt(t)
      assert(c < Token.Range)
      assert(c >= 0)
    }
  }
}
