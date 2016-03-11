package uk.ac.cam.eng.ml.tcs27.compression.tcs27;/* Automated copy from build process */
/* $Id: BitWriter.java,v 1.4 2015/07/30 15:58:56 chris Exp $ */

import java.io.IOException;

/** An interface for writing low-level bit streams. */
public interface BitWriter {
  
  /** Writes a single bit (0 or 1).
    * The bit is encoded in the least-significant bit position
    * of the byte. */
  public void writeBit(byte bit) throws IOException;
  
  /** Writes a single bit.
    * @see Bit */
  public default void writeBit(Bit b) throws IOException {
    writeBit(b.byteValue());
  }
  
  /** Closes the bit stream.
    * Any buffered bits are flushed before closing. */
  public void close() throws IOException;

}
