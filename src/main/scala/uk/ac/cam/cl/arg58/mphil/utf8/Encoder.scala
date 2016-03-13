package uk.ac.cam.cl.arg58.mphil.utf8

import java.io.OutputStream

/**
  * Created by adam on 13/03/16.
  */
object Encoder {
  private def computeNumOctets(codePoint: Int) : Int = {
    val res = UTF8.CodePoints.indexWhere(range => range.contains(codePoint))
    assert(res >= 0); assert(res <= 4)
    res + 1
  }

  private def codepointToBytes(codePoint: Int, numOctets: Int) : Array[Byte] = {
    val res = new Array[Byte](numOctets)
    val (firstOctet : Byte, codePointRem : Int) = numOctets match {
      case 1 => ((codePoint & 0x7f).toByte, codePoint >>> 7)
      case 2 => ((0xc0 | (codePoint & 0x1f)).toByte, codePoint >>> 5)
      case 3 => ((0xe0 | (codePoint & 0xf)).toByte, codePoint >>> 4)
      case 4 => ((0xf0 | (codePoint & 0x7)).toByte, codePoint >>> 3)
    }
    res(0) = firstOctet

    var codePointAcc = codePointRem
    for (octet <- 1 to numOctets-1) {
      res(octet) = (0x80 | (codePointAcc & 0x3f)).toByte
      codePointAcc = codePointAcc >>> 6
    }

    assert(codePointAcc == 0)
    res
  }

  private def encode(codePoint : Int) : Array[Byte] = {
    val numOctets = computeNumOctets(codePoint)
    codepointToBytes(codePoint, numOctets)
  }

  def tokenToBytes(token: Token) : Array[Byte] = token match {
    case UnicodeCharacter (codePoint) => encode(codePoint)
    case IllegalByte (byte) => Array(byte)
    case Overlong (codePoint, numOctets) => codepointToBytes(codePoint, numOctets)
    case SurrogateCodePoint (codePoint) => encode(codePoint)
    case TooHigh (codePoint) => codepointToBytes(codePoint, 4)
  }
}
