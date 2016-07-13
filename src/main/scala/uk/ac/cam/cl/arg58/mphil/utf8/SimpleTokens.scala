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

object STokenTypes extends Enumeration {
  val TUnicodeCharacter, TIllegalByte, TEOF = Value

  def fromToken(t: SimpleToken): STokenTypes.Value = t match {
    case _: SUnicodeCharacter => TUnicodeCharacter
    case _: SIllegalByte => TIllegalByte
    case _: SEOF => TEOF
  }

  def numTokens(t: STokenTypes.Value): Integer = t match {
    case TUnicodeCharacter => SUnicodeCharacter.Range
    case TIllegalByte => SIllegalByte.Range
    case TEOF => SEOF.Range
  }

  def toInt(t: SimpleToken): Int = t match {
    case c: SUnicodeCharacter => SUnicodeCharacter.toInt(c)
    case b: SIllegalByte => SIllegalByte.toInt(b)
    case e: SEOF => SEOF.toInt(e)
  }

  def ofInt(t: STokenTypes.Value, n: Int): SimpleToken = t match {
    case TUnicodeCharacter => SUnicodeCharacter.ofInt(n)
    case TIllegalByte => SIllegalByte.ofInt(n)
    case TEOF => SEOF.ofInt(n)
  }
}

abstract class SimpleToken

object SimpleToken {
  private final val ranges = Array(SUnicodeCharacter.Range, SIllegalByte.Range, SEOF.Range)
  private final val ofIntConversions = Array(SUnicodeCharacter.ofInt _, SIllegalByte.ofInt _, SEOF.ofInt _)

  final val Range = ranges.sum

  def ofInt(n: Int): SimpleToken = {
    var x = n
    for ((range, ofIntInRange) <- ranges zip ofIntConversions) {
      if (x < range) {
        return ofIntInRange(x)
      }
      x -= range
    }
    throw new AssertionError("n = " + n.toString + " too large.")
  }

  def toInt(t: SimpleToken): Int = t match {
    case c: SUnicodeCharacter => SUnicodeCharacter.toInt(c)
    case e: SIllegalByte => SUnicodeCharacter.Range + SIllegalByte.toInt(e)
    case eof: SEOF => SUnicodeCharacter.Range + SIllegalByte.Range + SEOF.toInt(eof)
  }
}

case class SUnicodeCharacter(codePoint: Int) extends SimpleToken {
  override def toString() = String.valueOf(Character.toChars(codePoint))
}

object SUnicodeCharacter {
  final val Range = 0x110000

  def apply(char: Char): SUnicodeCharacter = {
    val codePoint: Int = char.toInt
    assert(UTF8.CodePoints.exists(r => r.contains(codePoint)))
    SUnicodeCharacter(codePoint)
  }

  def ofInt(codePoint: Int): SUnicodeCharacter = {
    SUnicodeCharacter(codePoint)
  }

  def toInt(char: SUnicodeCharacter): Int = {
    char.codePoint
  }
}

case class SIllegalByte(byte: Byte) extends SimpleToken {
  override def toString() = "IllegalByte(%02X)".format(byte)
}

object SIllegalByte {
  // ASCII characters never illegal. All other bytes can be illegal (depending on context).
  // So 0xff - 0x7f = 0x80
  final val Range = 0x80

  def toInt(o: SIllegalByte): Int = {
    // Were o.byte unsigned, we would want to return o.byte - 0x80
    // However, it is signed with 0x80 = -127 and 0xff = -1.
    // So 0x80 + o.byte gives the appropriate result (0 at 0x80, going up to 0x7f for 0xff)
    0x80 + o.byte
  }

  def ofInt(n: Int): SIllegalByte = {
    SIllegalByte((n + 0x80).toByte)
  }
}


case class SEOF() extends SimpleToken {
  override def toString() = "EOF"
}

object SEOF {
  final val Range = 1

  def toInt(e: SEOF): Int = 0

  def ofInt(n: Int): SEOF = {
    assert(n == 0);
    SEOF()
  }
}