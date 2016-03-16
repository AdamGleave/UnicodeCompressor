package uk.ac.cam.cl.arg58.mphil.utf8

import java.lang.Character

/**
  * Created by adam on 11/03/16.
  */
abstract class Token

case class UnicodeCharacter(codePoint: Int) extends Token {
  def this(char: Char) = {
    this(char.toInt)
  }

  override def toString() = String.valueOf(Character.toChars(codePoint))
}

abstract class Error extends Token
case class IllegalByte(byte: Byte) extends Error {
  override def toString() = "IllegalByte(%02X)".format(byte)
}
abstract class IllegalCodePoint(codePoint: Int) extends Error {
  override def toString() = "%s(%X)".format(this.getClass.getSimpleName, codePoint)
}

case class SurrogateCodePoint(codePoint: Int) extends IllegalCodePoint(codePoint)
case class Overlong(codePoint: Int, length: Int) extends IllegalCodePoint(codePoint) {
  override def toString() = "Overlong(%X, %d)".format(codePoint, length)
}
case class TooHigh(codePoint: Int) extends IllegalCodePoint(codePoint)




