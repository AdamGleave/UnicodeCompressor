/* Automated copy from build process */
/* $Id: BitBuffer.java,v 1.8 2013/06/04 01:39:25 chris Exp $ */

import java.io.InputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.LinkedList;

/** A BitBuffer which buffers and relays bits written to it.
  * If more bits are read than have been written to the buffer,
  * a buffer underrun occurs and an exception is thrown. */
public class BitBuffer implements BitReader, BitWriter {
 
  // FIXME: currently 28 bits. Increase this to 64 bits?
  private static final int top    = 0x08000000;
  private static final int bottom = 0x00000001;
  private static final int zero   = 0x00000000;

  /** Current data word for reading. */
  private int rdata = 0x00000000;
  
  /** Current data word for writing. */
  private int wdata = 0x00000000;

  /** Current bit mask for reading. */
  private int rmask = zero;  // bit mask

  /** Current bit mask for writing. */
  private int wmask = zero;  // bit mask

  /** The internal integer queue. */
  private Queue<Integer> buf = null;

  /** Constructs a new (and empty) BitBuffer. */
  public BitBuffer() {
    buf = new LinkedList<Integer>();
  }

  /** Returns if this BitBuffer is empty.
    * @return true, if and only if the bit buffer is empty. */
  public boolean isEmpty() {
    return (wmask == zero) && buf.isEmpty();
  }

  /** Writes a bit into the buffer. */
  public void writeBit(byte b) {
    if (wmask == zero) {
      // start new word
      wmask = bottom;
      wdata = b;
    } else
    if (wmask == top) {
      // push current word into the buffer
      buf.add(wdata);
      wmask = bottom;
      wdata = b;
    } else {
      // add to current word
      wmask <<= 1;
      if (b != 0) {
        wdata |= wmask;
      }
    }
  }

  /** Reads a bit from the buffer. */
  public byte readBit() throws IOException {
    if (rmask == zero) {
      if (buf.isEmpty()) {
        // try to read from write cache directly
        if (wmask != zero) {
          byte snatch = (byte) ((wdata & 1) == 0 ? 0 : 1);
          wdata >>= 1;
          wmask >>= 1;
          return snatch;
        } else {
          // give up
          throw new IOException("BitBuffer underrun");
        }
      } else {
        // read next word
        rdata = buf.remove();
        rmask = bottom;
      }
    }
    byte out = (byte) ((rdata & rmask) == 0 ? 0 : 1);
    if (rmask == top) {
      rmask = zero;
    } else {
      rmask <<= 1;
    }
    return out;
  }

  public void close() {
    // ignore
  }
  
  public boolean informative() {
    return true;
  }

}
