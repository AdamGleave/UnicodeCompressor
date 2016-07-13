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

class SimpleTokenSpec extends FlatSpec  {
  "Tokens" should "be in bijection with the integers" in {
    for (i <- 0 to SimpleToken.Range - 1) {
      val t = SimpleToken.ofInt(i)
      assertResult(i, "token produced is [" + t + "]") {
        SimpleToken.toInt(t)
      }
    }
  }

  "Tokens" should "lie in the specified range" in {
    def check(i: Int): Unit = {
      assert(i < SimpleToken.Range)
      assert(i > 0)
    }

    val unicodeCharacters = (0 to 0x10ffff)
      .map(cp => SUnicodeCharacter(cp))
    val illegalBytes = (0x80 to 0xff).map(b => SIllegalByte(b.toByte))
    val eof = Array(SEOF())

    val allTokens = unicodeCharacters ++ illegalBytes ++ eof
    for (t <- allTokens) {
      val c = SimpleToken.toInt(t)
      assert(c < SimpleToken.Range)
      assert(c >= 0)
    }
  }
}
