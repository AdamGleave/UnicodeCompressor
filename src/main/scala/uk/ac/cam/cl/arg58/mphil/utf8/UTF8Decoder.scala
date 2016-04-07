package uk.ac.cam.cl.arg58.mphil.utf8

import java.io.InputStream
import java.util.NoSuchElementException

/**
  * Created by adam on 11/03/16.
  */
class UTF8Decoder(is: InputStream) extends Iterator[Token] {
  private val buffer = new Array[Byte](4)
  private var bytesRead = 0
  private var numTokens = 0

  private def consume(n : Int) {
    assert(n <= bytesRead)
    for (i <- 0 until bytesRead - n) {
      buffer(i) = buffer(i + n)
    }
    bytesRead -= n
  }

  private def multiOctet(seqLength : Int) : Token = {
    val firstByte = buffer(0)

    if (seqLength > bytesRead) {
      // hit EOF before we got enough bytes
      consume(1)
      IllegalByte(firstByte)
    } else {
      val bitsInTail = 7 - seqLength
      val mask = (1 << bitsInTail) - 1
      val tail = firstByte & mask

      var acc : Option[Int] = Some (tail)
      for (octet <- 1 to (seqLength - 1)) {
        val v = buffer(octet)
        if ((v & 0xc0) != 0x80) { // matches 10xxxxxx
          acc = None
        } else {
          acc = acc match {
            case None => None
            case Some (oldCodePoint) =>
              val newCodePoint = oldCodePoint << 6
              Some (newCodePoint + (v & 0x3f))
          }
        }
      }

      acc match {
        case None =>
          consume(1)
          IllegalByte(firstByte)
        case Some (codePoint) =>
          consume(seqLength)
          if (UTF8.SurrogateCodePoints.contains(codePoint)) {
            SurrogateCodePoint(codePoint)
          } else {
            val legalRange = UTF8.CodePoints(seqLength - 1)
            if (legalRange.contains(codePoint)) {
              UnicodeCharacter(codePoint)
            } else if (codePoint < legalRange.head) {
              Overlong(codePoint, seqLength)
            } else { // codePoint > legalRange.tail
              // this only happens if seqLength == 4, and codePoint > 10FFFF.
              TooHigh(codePoint)
            }
          }
      }
    }
  }

  def hasNext() : Boolean = {
    bytesRead += Math.max(is.read(buffer, bytesRead, buffer.length - bytesRead), 0)
    bytesRead > 0
  }

  def next() : Token = {
    bytesRead += Math.max(is.read(buffer, bytesRead, buffer.length - bytesRead), 0)
    if (bytesRead == 0) {
      throw new NoSuchElementException()
    } else {
      val firstByte : Byte = buffer(0)
      numTokens += 1
      if ((firstByte & 0x80) == 0x00) { // firstByte matches 0xxxxxxx
        consume(1)
        UnicodeCharacter (firstByte)
      } else if ((firstByte & 0xe0) == 0xc0) { // firstByte matches 110xxxxx
        multiOctet(2)
      } else if ((firstByte & 0xf0) == 0xe0) { // firstByte matches 1110xxxx
        multiOctet(3)
      } else if ((firstByte & 0xf8) == 0xf0) { // firstByte matches 11110xxx
        multiOctet(4)
      } else {
        consume(1)
        IllegalByte (firstByte)
      }
    }
  }
}