package uk.ac.cam.cl.arg58.mphil.utf8

import java.io.InputStream

/**
  * Created by adam on 11/03/16.
  */
class Decoder(is: InputStream) {
  private val buffer = new Array[Byte](4)
  private var bytesRead = 0

  private def consume(n : Int) {
    val m = math.min(bytesRead, n)
    for (i <- 0 to bytesRead - m) {
      buffer(i) = buffer(i + m)
    }
    bytesRead -= m
  }

  def multiOctet(octets : Int) : Option[Int] = {
    var res : Option[Int] = Some (0)
    for (octet <- octets to 1) {
      val v = buffer(octet)
      if ((octet & 0xc0) != 0x80) { // matches 10xxxxxx
        res = None
      } else {
        res = res match {
          case None => None
          case Some (old_codepoint) =>
            val new_codepoint = old_codepoint << 6
            Some (new_codepoint + (octet & 0x3f))
        }
      }
    }
    res
  }

  def nextToken() : Option[Token] = {
    bytesRead += is.read(buffer, bytesRead, buffer.length - bytesRead)
    if (bytesRead == 0) {
      None
    } else {
      val firstByte = buffer(0)
      if ((firstByte & 0x80) == 0x00) { // firstByte matches 0xxxxxxx
        Some (Character(firstByte))
      } else if ((firstByte & 0xe0) == 0xc0) { // firstByte matches 110xxxxx
        multiOctet(2) match {
          case None =>
            consume(1)
            Some(IllegalByte(firstByte))
          case Some(codepoint) =>
            consume(2)
            Some(Character(codepoint))
        }
      } else {
        None // TODO
      }
    }
  }
}
