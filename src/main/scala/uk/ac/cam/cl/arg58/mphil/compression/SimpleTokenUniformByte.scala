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

package uk.ac.cam.cl.arg58.mphil.compression

import java.util.Random

import uk.ac.cam.eng.ml.tcs27.compression.SimpleMass

/*
  Prior over SimpleTokens that assigns probability to each token equal to the chance it would be
  decoded in a uniformly random byte stream.
 */
object SimpleTokenUniformByte extends SimpleMass[Integer] with IntegerMass {
  val masses = Array(
    // Pair(end (exclusive), mass per integer)
    //// UnicodeCharacters
    // 1-byte codewords (ASCII), can appear after any sequence of three bytes
    Pair(0x80, 256 * 256 * 256),
    // 2-byte codewords, can appear after any sequence of two bytes
    Pair(0x800, 256 * 256),
    // 3-byte codewords, can appear after any byte
    Pair(0x10000, 256),
    // 4-byte codeword
    Pair(0x110000, 1),
    //// IllegalBytes, between 128 to 255
    // Continuation bytes: 0b10xxxxxx, ranging from 128=0x80=0b10000000 to 0b10111111=191=0xbf
    // Can occur *legally* after:
    // - Any two bytes, then a leading byte 0b110xxxxx. # of sequences: 256 * 256 * 32 = 0x200000.
    // - Any byte, then a leading byte 0b1110xxxx, then a continuation byte 0b10xxxxxx.
    //   # of sequences: 256 * 16 * 64 = 0x40000
    // - A leading byte 0b11110xxx, then two continuation bytes 0b10xxxxxx.
    //   # of sequences: 8 * 64 * 64 = 0x8000.
    // So occurs illegally in 256 * 256 * 256 - (0x200000 + 0x40000 + 0x8000) = 0xdb8000 cases.
    Pair(0x110000 + 0xc0, 0xdb8000),
    // Leading bytes for 2-byte codewords 0b110xxxxx, ranging from 0xc0 to 0xdf.
    // Occurs legally when followed by a continuation byte, 0b10xxxxxx.
    // So occurs illegally when followed by any of the other 192 bytes.
    Pair(0x110000 + 0xdf, 192 * 256 * 256),
    // Leading byte for 3-byte codewords 1110xxxx, ranging from 0xe0 to 0xef
    // Occurs legally when followed by two continuation bytes
    // So occurs illegally if the first byte is one of the 192 non-continuation bytes: 192 * 256 * 256
    // Or if the first byte is one of the 64 legal continuation bytes, but the second byte isn't: 64 * 192 * 256.
    Pair(0x10ffff + 0xf0, 320 * 192 * 256),
    // Leading byte for 4-byte codewords 11110xxx, ranging from 0xf0 to 0xf7
    // Occurs legally when followed by three continuation bytes
    // So occurs illegally if first byte is non-continuation: 192 * 256 * 256
    // Or if first byte is continuation, and second byte isn't: 64 * 192 * 256
    // Or if first and second byte are continuation, but third byte isn't: 64 * 64 * 192
    Pair(0x10ffff + 0xf8, 192 * (256*256 + 64*256 + 64*64)),
    // All other bytes, 0xf8 to 0xff, are illegal in any context
    Pair(0x10ffff + 0x100, 256 * 256 * 256)
  )
  val totalMass = masses.fold((0,0)) { (acc, x) =>
    acc match { case (start, s) =>
      x match { case (end, m) =>
        (end, s + (end - start) * m)
      }
    }
  }._2
  val logTotalMass = Math.log(totalMass)

  def mass(x: Integer) : Double = {
    masses.find(p => x < p._1) match {
      case Some(x) =>
        x._2.toDouble / totalMass
      case None =>
        0.0
    }
  }
  override def logMass(x: Integer): Double = {
    masses.find(p => x < p._1) match {
      case Some(x) =>
        Math.log(x._2.toDouble) - logTotalMass
      case None =>
        Double.NegativeInfinity
    }
  }

  // start and end inclusive
  override def discreteMassBetween(start: Integer, end: Integer): Int = {
    var acc = 0
    var start_from = start

    // range_end exclusive
    for ((range_end, mass) <- masses) {
      val nearest_end = Math.min(end + 1, range_end)
      val left_in_range = nearest_end - start
      if (left_in_range > 0) {
        acc += left_in_range * mass
        start_from = nearest_end
      }
    }

    acc
  }

  override def massBetween(start: Integer, end: Integer): Double =
    discreteMassBetween(start, end) / totalMass

  override def sample(rnd: Random): Integer = throw new AssertionError("unimplemented")
}
