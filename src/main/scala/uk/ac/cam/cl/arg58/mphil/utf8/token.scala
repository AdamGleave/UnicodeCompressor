package uk.ac.cam.cl.arg58.mphil.utf8

object TokenTypes extends Enumeration {
  val TUnicodeCharacter, TIllegalByte, TOverlong, TSurrogateCodePoint, TTooHigh, TEOF = Value

  def fromToken(t: Token): TokenTypes.Value = t match {
    case _: UnicodeCharacter => TUnicodeCharacter
    case _: IllegalByte => TIllegalByte
    case _: Overlong => TOverlong
    case _: SurrogateCodePoint => TSurrogateCodePoint
    case _: TooHigh => TTooHigh
    case _: EOF => TEOF
  }

  def numTokens(t: TokenTypes.Value): Integer = t match {
    case TUnicodeCharacter => UnicodeCharacter.Range
    case TIllegalByte => IllegalByte.Range
    case TOverlong => Overlong.Range
    case TSurrogateCodePoint => SurrogateCodePoint.Range
    case TTooHigh => TooHigh.Range
    case TEOF => EOF.Range
  }

  def toInt(t: Token): Int = t match {
    case c: UnicodeCharacter => UnicodeCharacter.toInt(c)
    case b: IllegalByte => IllegalByte.toInt(b)
    case o: Overlong => Overlong.toInt(o)
    case s : SurrogateCodePoint => SurrogateCodePoint.toInt(s)
    case t: TooHigh => TooHigh.toInt(t)
    case e: EOF => EOF.toInt(e)
  }

  def ofInt(t: TokenTypes.Value, n: Int): Token = t match {
    case TUnicodeCharacter => UnicodeCharacter.ofInt(n)
    case TIllegalByte => IllegalByte.ofInt(n)
    case TOverlong => Overlong.ofInt(n)
    case TSurrogateCodePoint => SurrogateCodePoint.ofInt(n)
    case TTooHigh => TooHigh.ofInt(n)
    case TEOF => EOF.ofInt(n)
  }
}

abstract class Token

object Token {
  private final val ranges = Array(UnicodeCharacter.Range, Error.Range, EOF.Range)
  private final val ofIntConversions = Array(UnicodeCharacter.ofInt _, Error.ofInt _, EOF.ofInt _)

  final val Range = ranges.sum

  def ofInt(n: Int): Token = {
    var x = n
    for ((range, ofIntInRange) <- ranges zip ofIntConversions) {
      if (x < range) {
        return ofIntInRange(x)
      }
      x -= range
    }
    throw new AssertionError("n = " + n.toString + " too large.")
  }

  def toInt(t: Token): Int = t match {
    case c: UnicodeCharacter => UnicodeCharacter.toInt(c)
    case e: Error => UnicodeCharacter.Range + Error.toInt(e)
    case eof: EOF => UnicodeCharacter.Range + Error.Range + EOF.toInt(eof)
  }
}


case class UnicodeCharacter(codePoint: Int) extends Token {
  override def toString() = String.valueOf(Character.toChars(codePoint))
}

object UnicodeCharacter {
  // computed from 0x110000 - 0x800
  // 0x110000 = 0x10ffff (the UTF-8 max code point) + 1. 0x800 is the number of surrogate codepoints
  final val Range = 1112064

  def apply(char: Char): UnicodeCharacter = {
    val codePoint: Int = char.toInt
    assert(UTF8.CodePoints.exists(r => r.contains(codePoint)))
    UnicodeCharacter(codePoint)
  }

  def ofInt(codePoint: Int): UnicodeCharacter = {
    if (codePoint < UTF8.SurrogateCodePoints.head) {
      UnicodeCharacter(codePoint)
    } else {
      UnicodeCharacter(codePoint + UTF8.SurrogateCodePoints.size)
    }
  }

  def toInt(char: UnicodeCharacter): Int = {
    val codePoint = char.codePoint
    if (codePoint < UTF8.SurrogateCodePoints.head) {
      codePoint
    } else if (codePoint > UTF8.SurrogateCodePoints.end) {
      codePoint - UTF8.SurrogateCodePoints.size
    } else {
      throw new AssertionError("Bug.")
    }
  }
}


abstract class Error extends Token

object Error {
  private final val ranges = Array(IllegalByte.Range, IllegalCodePoint.Range)
  private final val ofIntConversions = Array(IllegalByte.ofInt _, IllegalCodePoint.ofInt _)

  final val Range = ranges.sum

  def ofInt(n: Int): Error = {
    var x = n
    for ((range, ofIntInRange) <- ranges zip ofIntConversions) {
      if (x < range) {
        return ofIntInRange(x)
      }
      x -= range
    }
    throw new AssertionError("n = " + n.toString + " is too large.")
  }

  def toInt(t: Token): Int = t match {
    case b: IllegalByte => IllegalByte.toInt(b)
    case c: IllegalCodePoint => IllegalByte.Range + IllegalCodePoint.toInt(c)
  }
}


case class IllegalByte(byte: Byte) extends Error {
  override def toString() = "IllegalByte(%02X)".format(byte)
}

object IllegalByte {
  // ASCII characters never illegal. All other bytes can be illegal (depending on context).
  // So 0xff - 0x7f = 0x80
  final val Range = 0x80

  def toInt(o: IllegalByte): Int = {
    // Were o.byte unsigned, we would want to return o.byte - 0x80
    // However, it is signed with 0x80 = -127 and 0xff = -1.
    // So 0x80 + o.byte gives the appropriate result (0 at 0x80, going up to 0x7f for 0xff)
    0x80 + o.byte
  }

  def ofInt(n: Int): IllegalByte = {
    IllegalByte((n + 0x80).toByte)
  }
}


abstract class IllegalCodePoint(codePoint: Int) extends Error {
  override def toString() = "%s(%X)".format(this.getClass.getSimpleName, codePoint)
}

object IllegalCodePoint {
  private final val ranges = Array(SurrogateCodePoint.Range, Overlong.Range, TooHigh.Range)
  private final val ofIntConversions = Array(SurrogateCodePoint.ofInt _,
                                             Overlong.ofInt _,
                                             TooHigh.ofInt _)

  final val Range = ranges.sum

  def ofInt(n: Int): IllegalCodePoint = {
    var x = n
    for ((range, ofIntInRange) <- ranges zip ofIntConversions) {
      if (x < range) {
        return ofIntInRange(x)
      }
      x -= range
    }
    throw new AssertionError("n = " + n.toString + " is too large.")
  }

  def toInt(t: Token): Int = t match {
    case s: SurrogateCodePoint => SurrogateCodePoint.toInt(s)
    case o: Overlong => SurrogateCodePoint.Range + Overlong.toInt(o)
    case t: TooHigh => SurrogateCodePoint.Range + Overlong.Range + TooHigh.toInt(t)
  }
}


case class SurrogateCodePoint(codePoint: Int) extends IllegalCodePoint(codePoint)

object SurrogateCodePoint {
  final val Range = 2048 // computed from UTF8.SurrogateCodePoints.size

  def toInt(o: SurrogateCodePoint): Int = {
    o.codePoint - UTF8.SurrogateCodePoints.head
  }

  def ofInt(n: Int): SurrogateCodePoint = {
    SurrogateCodePoint(n + UTF8.SurrogateCodePoints.head)
  }
}


case class Overlong(codePoint: Int, length: Int) extends IllegalCodePoint(codePoint) {
  override def toString() = "Overlong(%X, %d)".format(codePoint, length)
}

object Overlong {
  // computed from UTF8.CodePoints.zipWithIndex.map({ case (r, i) => (r.size * (3-i))}).sum
  final val AccumulatedSizes = UTF8.CodePoints
    .zipWithIndex
    .map({ case (r, i) => r.size * (3 - i)})
    .scanLeft(0)(_ + _)
  final val Range = AccumulatedSizes.last

  def toInt(o: Overlong): Int = {
    val (range, index) = UTF8.CodePoints
      .zipWithIndex
      .find({case (r, i) => r.contains(o.codePoint)})
      .get
    val correctLength = index + 1
    val previousSize = AccumulatedSizes(index)
    val offset = o.codePoint - range.start
    val overlongBy = o.length - correctLength
    previousSize + (overlongBy - 1) * range.size + offset
  }

  def ofInt(n: Int): Overlong = {
    val (size, index) = AccumulatedSizes
      .zipWithIndex
      .find({case (s, i) => n < s})
      .get
    val previousSize = AccumulatedSizes(index - 1)
    val range = UTF8.CodePoints(index - 1)
    val width = range.size
    val offset = n - previousSize
    val correctLength = index
    val length = offset / width + correctLength + 1
    val codePoint = offset % width + range.start
    Overlong(codePoint, length)
  }
}


case class TooHigh(codePoint: Int) extends IllegalCodePoint(codePoint)

object TooHigh {
  // TooHigh if 0x10ffff < codePoint <= 0x1fffff
  // So range is 0x1fffff - 0x10ffff = 0xf0000
  final val Range = 0xf0000

  def toInt(o: TooHigh): Int = {
    o.codePoint - 0x110000
  }

  def ofInt(n: Int): TooHigh = {
    TooHigh(n + 0x110000)
  }
}


case class EOF() extends Token {
  override def toString() = "EOF"
}

object EOF {
  final val Range = 1

  def toInt(e: EOF): Int = 0

  def ofInt(n: Int): EOF = {
    assert(n == 0);
    EOF()
  }
}