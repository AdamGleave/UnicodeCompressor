/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.io.IOException;

/** A BitReader producing an infinite sequence of bits of
  * a specified value (0 or 1).
  * @see RandomBitReader */
public class ConstantBitReader implements BitReader {

  byte bit;

  /** Constructs a BitReader providing an arbitrary amount of 
    * zero-bits. */
  public ConstantBitReader() {
    this.bit = 0;
  }

  /** Constructs a new BitReader providing an infinite number
    * of bits of the specified value. */
  public ConstantBitReader(byte bit) {
    if (bit == 0 || bit == 1) {
      this.bit = bit;
    } else {
      throw new IllegalArgumentException("invalid bit value");
    }
  }

  /** Returns the next bit.
    * @return the bit constant as specified at construction time */
  public byte readBit() {
    return bit;
  }

  /** Closes this ConstantBitReader. */
  public void close() {
    // nothing to do
  }

  public boolean informative() {
    return false;
  }

}
