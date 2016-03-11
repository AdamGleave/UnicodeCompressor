package uk.ac.cam.eng.ml.tcs27.compression.tcs27;/* Automated copy from build process */
/* $Id: BitReader.java,v 1.4 2012/11/18 00:00:26 chris Exp $ */

import java.io.IOException;

/** An interface for reading low-level bit streams. */
public interface BitReader {
  
  /** Reads a single bit (0 or 1).
    * The bit is encoded in the least-significant bit of the
    * returned byte, the upper 7 bits are zero.
    * @return the bit */
  public byte readBit() throws IOException;

  /** Closes the stream. */
  public void close() throws IOException;

  /** Returns whether the bits read from this source
    * are still worth reading.
    * Some bit sources may send an infinite supply of
    * zeros after the interesting content has been exhausted.
    * In this case, this method can hint that following
    * bits are no longer informative. */
  public boolean informative();

}
