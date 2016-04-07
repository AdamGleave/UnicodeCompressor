package uk.ac.cam.cl.arg58.mphil.utf8

import java.lang.Character

/**
  * Created by adam on 11/03/16.
  */

object Token {
  final val TotalRange = 2164993

  def toInt(t: Token) : Int = t match {
    case UnicodeCharacter(codePoint) => codePoint
    case SurrogateCodePoint(codePoint) => codePoint
    case TooHigh(codePoint) => codePoint
    case Overlong(codePoint, length) =>
      val (range, index) = UTF8.CodePoints
        .zipWithIndex
        .find({case (r, i) => r.contains(codePoint)})
        .get
      val correct_length = index + 1
      val offset = codePoint - range.start
      val overlong_by = length - correct_length
      0x200000 + (overlong_by * range.size) + codePoint
    case IllegalByte(byte) => (0x200000 + 67712) + (byte - 0x7f)
    case EOF() => Token.TotalRange
  }

  def ofInt(n: Int): Token = {
    if (n <= 0x1fffff) {
      val codePoint = n
      if (UTF8.SurrogateCodePoints.contains(codePoint)) {
        SurrogateCodePoint(codePoint)
      } else if (codePoint > 0x10ffff) {
        TooHigh(codePoint)
      } else {
        UnicodeCharacter(codePoint)
      }
    } else if (n < 0x200000 + 67712) {
      val overlongCodePoint = n - 0x200000
      var index = 0
      var size = 0
      var previousSize = 0
      do {
        previousSize = size
        size += UTF8.CodePoints(index).size * (3 - index)
        index += 1
      } while (size < overlongCodePoint)
      val width = UTF8.CodePoints(index - 1).size
      val offset = overlongCodePoint - previousSize
      val correct_length = index
      val length = offset / width + correct_length
      val codePoint = overlongCodePoint % width
      Overlong(codePoint, length)
    } else if (n <= 0x200000 + 67712 + 0x80) {
      val byte = n - (0x200000 + 67712)
      IllegalByte(byte.toByte)
    } else if (n == Token.TotalRange) {
      EOF()
    } else {
      throw new AssertionError("Bug: n == " + n.toString)
    }
  }
}

abstract class Token {
  val Range : Int
}

case class UnicodeCharacter(codePoint: Int) extends Token {
  final val Range = 1114112 // computed from UTF8.CodePoints.map(r => r.size).sum

  def apply(char: Char) = {
    val codePoint: Int = char.toInt
    assert(UTF8.CodePoints.exists(r => r.contains(codePoint)))
    new UnicodeCharacter(codePoint)
  }

  override def toString() = String.valueOf(Character.toChars(codePoint))
}

abstract class Error extends Token

case class IllegalByte(byte: Byte) extends Error {
  // ASCII characters never illegal. All other bytes can be illegal (depending on context).
  // So 0xff - 0x7f = 0x80
  final val Range = 0x80

  override def toString() = "IllegalByte(%02X)".format(byte)
}

abstract class IllegalCodePoint(codePoint: Int) extends Error {
  override def toString() = "%s(%X)".format(this.getClass.getSimpleName, codePoint)
}

case class SurrogateCodePoint(codePoint: Int) extends IllegalCodePoint(codePoint) {
  final val Range = 2048 // computed from UTF8.SurrogateCodePoints.size
}

case class Overlong(codePoint: Int, length: Int) extends IllegalCodePoint(codePoint) {
  // computed from UTF8.CodePoints.zipWithIndex.map({ case (r, i) => (r.size * (3-i))}).sum
  final val Range = 67712

  override def toString() = "Overlong(%X, %d)".format(codePoint, length)
}
case class TooHigh(codePoint: Int) extends IllegalCodePoint(codePoint) {
  // TooHigh if 0x10ffff < codePoint <= 0x1fffff
  // So range is 0x1fffff - 0x10ffff = 0xf0000
  final val Range = 0xf0000
}

case class EOF() extends Token {
  final val Range = 1

  override def toString() = "EOF"
}