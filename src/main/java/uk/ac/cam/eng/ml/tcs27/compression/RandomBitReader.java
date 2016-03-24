/* Automated copy from build process */
/* $Id: RandomBitReader.java,v 1.3 2012/11/18 00:00:26 chris Exp $ */

import java.io.IOException;
import java.util.Random;

/** A BitReader producing an infinite sequence of random bits.
  * Random bits are drawn from the random source supplied at
  * construction time. */
public class RandomBitReader implements BitReader {
  
  /** Random source for this RandomBitReader. */
  Random rnd = null;

  /** Constructs a new RandomBitReader. */
  public RandomBitReader(Random src) {
    this.rnd = src;
  }

  /** Returns a uniformly random bit (0 or 1).
    * The bit is encoded in the least-significant bit of the
    * returned byte, the upper 7 bits are zero.
    * @return the random bit */
  public byte readBit() {
    return rnd.nextBoolean() ? (byte) 1 : (byte) 0;
  }

  /** Closes this RandomBitReader. */
  public void close() {
    // nothing to do
  }

  /** Random bits are always informative. */
  public boolean informative() {
    return true;
  }

}
