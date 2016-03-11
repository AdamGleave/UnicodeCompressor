package uk.ac.cam.cl.arg58.mphil.utf8

/**
  * Created by adam on 11/03/16.
  */
abstract class Token

case class Character(codePoint: Int) extends Token

abstract class Error extends Token
// TODO
case class Overlong(codePoint: Int, length: Int) extends Error
case class IllegalCodepoint(codePoint: Int) extends Error
case class IllegalByte(byte: Byte) extends Error


