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

class DetailedTokenSpec extends FlatSpec  {
  "Tokens" should "be in bijection with the integers" in {
    for (i <- 0 to DetailedToken.Range - 1) {
      val t = DetailedToken.ofInt(i)
      assertResult(i, "token produced is [" + t + "]") {
        DetailedToken.toInt(t)
      }
    }
  }

  "Tokens" should "lie in the specified range" in {
    def check(i: Int): Unit = {
      assert(i < DetailedToken.Range)
      assert(i > 0)
    }

    val unicodeCharacters = (0 to 0x10ffff)
      .filter(cp => !UTF8.SurrogateCodePoints.contains(cp))
      .map(cp => DUnicodeCharacter(cp))
    val surrogates = UTF8.SurrogateCodePoints.map(cp => SurrogateCodePoint(cp))
    val overlong =
      for ((range, index) <- UTF8.CodePoints.zipWithIndex;
            wrong_length <- (index + 2) to 4;
            cp <- range)
        yield Overlong(cp, wrong_length)
    val tooHigh = (0x110000 to 0x1fffff).map(cp => TooHigh(cp))
    val illegalBytes = (0x80 to 0xff).map(b => DIllegalByte(b.toByte))
    val eof = Array(DEOF())

    val allTokens = unicodeCharacters ++ surrogates ++ overlong ++ tooHigh ++ illegalBytes ++ eof
    for (t <- allTokens) {
      val c = DetailedToken.toInt(t)
      assert(c < DetailedToken.Range)
      assert(c >= 0)
    }
  }
}
