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

import java.io.OutputStream

object DetailedUTF8Encoder {
  private def computeNumOctets(codePoint: Int) : Int = {
    val res = UTF8.CodePoints.indexWhere(range => range.contains(codePoint))
    assert(res >= 0); assert(res <= 4)
    res + 1
  }

  private def codepointToBytes(codePoint: Int, numOctets: Int) : Array[Byte] = {
    val res = new Array[Byte](numOctets)

    var cp = codePoint
    for (octet <- (numOctets - 1) to 1 by -1) {
      res(octet) = (0x80 | (cp & 0x3f)).toByte
      cp = cp >>> 6
    }

    val (firstOctet : Byte, cpRem : Int) = numOctets match {
      case 1 => ((cp & 0x7f).toByte, cp >>> 7)
      case 2 => ((0xc0 | (cp & 0x1f)).toByte, cp >>> 5)
      case 3 => ((0xe0 | (cp & 0xf)).toByte, cp >>> 4)
      case 4 => ((0xf0 | (cp & 0x7)).toByte, cp >>> 3)
    }
    res(0) = firstOctet

    assert(cpRem == 0)
    res
  }

  private def encode(codePoint : Int) : Array[Byte] = {
    val numOctets = computeNumOctets(codePoint)
    codepointToBytes(codePoint, numOctets)
  }

  def tokenToBytes(token: DetailedToken) : Array[Byte] = token match {
    case DUnicodeCharacter(codePoint) => encode(codePoint)
    case DIllegalByte (byte)=> Array(byte)
    case Overlong(codePoint, numOctets) => codepointToBytes(codePoint, numOctets)
    case SurrogateCodePoint(codePoint) => encode(codePoint)
    case TooHigh(codePoint) => codepointToBytes(codePoint, 4)
    case DEOF() => throw new AssertionError("EOF has no byte representation.")
  }
}
