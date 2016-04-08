package uk.ac.cam.cl.arg58.mphil.utf8

/**
  * Created by adam on 13/03/16.
  */

import scala.io.Source
import java.io.{FileInputStream, ByteArrayOutputStream, ByteArrayInputStream}

import org.scalatest._

import scala.reflect.ClassTag

class CoderSpec extends FlatSpec {
  private def padArray[T](xs: Array[T], pad: T)(implicit m: ClassTag[T]) : Array[T] = {
    val res = new Array[T](xs.length * 2)
    for (i <- 0 to xs.length - 1) {
      res(2*i) = xs(i)
      res(2*i + 1) = pad
    }
    res
  }

  // many of these test cases due to Markus Kuhn,
  // see https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
  val testCases = Map(
    "κόσμε".getBytes("UTF-8") -> Array(UnicodeCharacter('κ'), UnicodeCharacter('ό'), UnicodeCharacter('σ'),
      UnicodeCharacter('μ'), UnicodeCharacter('ε')),

    //// single character tests
    // first possible sequence of certain length
    "\u0000".getBytes("UTF-8") -> Array(UnicodeCharacter(0x0000)), // 1 octet
    "\u0080".getBytes("UTF-8") -> Array(UnicodeCharacter(0x0080)), // 2 octets
    "\u0800".getBytes("UTF-8") -> Array(UnicodeCharacter(0x0800)), // 3 octets
    "\uD800\uDC00".getBytes("UTF-8") -> Array(UnicodeCharacter(0x10000)), // 4 octets

    // last possible sequence of certain length
    "\u007f".getBytes("UTF-8") -> Array(UnicodeCharacter(0x007f)), // 1 octet
    "\u07ff".getBytes("UTF-8") -> Array(UnicodeCharacter(0x07ff)), // 2 octets
    "\uffff".getBytes("UTF-8") -> Array(UnicodeCharacter(0xffff)), // 3 octets
    // 4 octets
    // largest legal codepoint
    "\uDBFF\uDFFF".getBytes("UTF-8") -> Array(UnicodeCharacter(0x10ffff)),
    // boundary: illegal codepoint
    Array(0xf4.toByte, 0x90.toByte, 0x80.toByte, 0x80.toByte) -> Array(TooHigh(0x110000)),
    // largest codepoint that can be expressed in 4 bytes
    Array(0xf7.toByte, 0xbf.toByte, 0xbf.toByte, 0xbf.toByte) -> Array(TooHigh(0x1fffff)),

    // illegal bytes
    Array(0xfe.toByte) -> Array(IllegalByte(0xfe.toByte)),

    // surrogates
    "\ud7ff".getBytes("UTF-8") -> Array(UnicodeCharacter(0xd7ff)), // below high surrogate
    // Scala is too clever to encode surrogate codepoints to UTF-8 (they're illegal), so specify byte array manually
    Array(0xed.toByte, 0xa0.toByte, 0x80.toByte) -> Array(SurrogateCodePoint(0xd800)), // first high surrogate
    Array(0xed.toByte, 0xa6.toByte, 0xa3.toByte) -> Array(SurrogateCodePoint(0xd9a3)), // random surrogate
    Array(0xed.toByte, 0xbf.toByte, 0xbf.toByte) -> Array(SurrogateCodePoint(0xdfff)), // last low surrogate
    "\ue000".getBytes("UTF-8") -> Array(UnicodeCharacter(0xe000)), // above low surrogate

    //// malformed sequences
    // unexpected continuation bytes
    Array(0x80.toByte) -> Array(IllegalByte(0x80.toByte)),
    Array(0xbf.toByte) -> Array(IllegalByte(0xbf.toByte)),
    Array(0x80.toByte, 0xbf.toByte) -> Array(IllegalByte(0x80.toByte), IllegalByte(0xbf.toByte)),
    // sequence of all 64 possible continuation bytes
    (0x80 to 0xbf).toArray.map((x: Int) => x.toByte) -> (0x80 to 0xbf).toArray.map((x: Int) => IllegalByte(x.toByte)),

    // lonely start characters
    padArray[Byte]((0xc0 to 0xfd).toArray.map((x: Int) => x.toByte), 0x20.toByte) ->
      padArray[Token]((0xc0 to 0xfd).toArray.map((x: Int) => IllegalByte(x.toByte) : Token),
        UnicodeCharacter(0x20) : Token),

    // sequences with last continuation byte missing
    // U+0000
    Array(0xc0.toByte) -> Array(IllegalByte(0xc0.toByte)), // 2 byte
    Array(0xe0.toByte, 0x80.toByte) -> Array(IllegalByte(0xe0.toByte), IllegalByte(0x80.toByte)), // 3 byte
    Array(0xf0.toByte, 0x80.toByte, 0x80.toByte) ->
      Array(IllegalByte(0xf0.toByte), IllegalByte(0x80.toByte), IllegalByte(0x80.toByte)), // 3 byte
    "\u07ff".getBytes("UTF-8").dropRight(1) -> Array(IllegalByte(0xdf.toByte)), // 2 byte, 7fff
    "\uffff".getBytes("UTF-8").dropRight(1) ->
      Array(IllegalByte(0xef.toByte), IllegalByte(0xbf.toByte)), // 3 byte, ffff
    "\udbff\udfff".getBytes("UTF-8").dropRight(1) ->
      Array(IllegalByte(0xf4.toByte), IllegalByte(0x8f.toByte), IllegalByte(0xbf.toByte)), // 4 byte, 10ffff

    //// overlong sequences
    // slash character, 0x2f
    Array(0xc0.toByte, 0xaf.toByte) -> Array(Overlong(0x2f, 2)),
    Array(0xe0.toByte, 0x80.toByte, 0xaf.toByte) -> Array(Overlong(0x2f, 3)),
    Array(0xf0.toByte, 0x80.toByte, 0x80.toByte, 0xaf.toByte) -> Array(Overlong(0x2f, 4)),
    // maximum overlong sequence: highest Unicode code value that is still overlong
    Array(0xc1.toByte, 0xbf.toByte) -> Array(Overlong(0x7f, 2)),
    Array(0xe0.toByte, 0x9f.toByte, 0xbf.toByte) -> Array(Overlong(0x07ff, 3)),
    Array(0xf0.toByte, 0x8f.toByte, 0xbf.toByte, 0xbf.toByte) -> Array(Overlong(0xffff, 4))
  )

  "A decoder" should "produce the tokens in testCases" in {
    for ((utf8Input, expectedOutput) <- testCases) {
      assertResult(expectedOutput, "input was [" + utf8Input.deep.mkString(",") + "]") {
        val is = new ByteArrayInputStream(utf8Input)
        val d = new UTF8Decoder(is)
        d.toArray
      }
    }
  }

  "An encoder" should "produce the bytes in testCases" in {
    for ((expectedOutput, tokenInput) <- testCases) {
      assertResult(expectedOutput, "input was [" + tokenInput.deep.mkString(", ") + "]") {
        tokenInput.flatMap(UTF8Encoder.tokenToBytes)
      }
    }
  }

  // in test resources
  val utf8Files = Array(
    "/corpora/unit/kuhn-baddata.txt",
    "/corpora/unit/kuhn-demo.txt",
    "/corpora/unit/kuhn-quickbrown.txt",
    "/corpora/unit/icaneatglass.txt",
    "/corpora/unit/beowulf.txt"
  )

  "A decoder and encoder" should "be the identity under composition" in {
    for (fname <- utf8Files) {
      val is1 = getClass.getResourceAsStream(fname)
      val d = new UTF8Decoder(is1)
      val decodedThenEncoded = d.flatMap(UTF8Encoder.tokenToBytes).toStream

      val is2 = getClass.getResourceAsStream(fname)
      val original = Stream.continually(is2.read).takeWhile(-1 !=).map(_.toByte)

      assert(decodedThenEncoded === original, "in file " + fname)
    }
  }
}
