/* Automated copy from build process */
/* $Id: CountingBitBuffer.java,v 1.1 2012/03/25 09:43:58 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.io.InputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;

/** A BitBuffer which counts the number of bits that are
  * written to and read from it.
  * @see BitCounter */
public class CountingBitBuffer extends BitBuffer implements
                                   BitReader, BitWriter {
 
  /** Number of bits written. */
  protected long wcount = 0;
  /** Number of bits read. */
  protected long rcount = 0;

  /** Constructs a new (and empty) CountingBitBuffer. */
  public CountingBitBuffer() {
    super();
  }

  /** Writes a bit into the buffer. */
  public void writeBit(byte b) {
    wcount++;
    super.writeBit(b);
  }

  /** Reads a bit from the buffer. */
  public byte readBit() throws IOException {
    rcount++;
    return super.readBit();
  }

  /** Returns the number of bits written to this CountingBitBuffer. */
  public long bitsWritten() {
    return wcount;
  }
  
  /** Returns the number of bits read from this CountingBitBuffer. */
  public long bitsRead() {
    return rcount;
  }

  public void close() {
    // ignore
  }

}
