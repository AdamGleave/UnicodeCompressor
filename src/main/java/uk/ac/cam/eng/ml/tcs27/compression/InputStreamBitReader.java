/* Automated copy from build process */
/* $Id: InputStreamBitReader.java,v 1.3 2015/07/31 15:10:42 chris Exp $ */

import java.io.InputStream;
import java.io.IOException;

/** A BitReader which reads bits from a given InputStream.
  * After the source InputStream has reached its end, the BitReader
  * produces zero-bits for any subsequent bit reads. */
public class InputStreamBitReader implements BitReader {
  
  /** Bit source. */
  private InputStream source = null;

  /** Current data byte. */
  private int data = 0x00;

  /** Current bit mask. */
  private int mask = 0x00;  // bit mask

  /** Indicator if the bit source is still active. */
  private boolean more = true;

  /** Constructs a new BitReader which reads bits from given InputStream. */
  public InputStreamBitReader(InputStream is) {
    this.source = is;
  }

  public byte readBit() throws IOException {
    if (more) {
      if (mask == 0x00) {
        // read next byte
        int next = source.read();
        if (next != -1) {
          data = next;
          mask = 0x80;
        } else {
          // end of stream -- only zero-bits from now
          more = false;
          data = (byte) 0x00;
          mask = (byte) 0x00;
        }
      }
      byte out = (byte) ((data & mask) == 0 ? 0 : 1);
      mask >>= 1;
      return out;
    } else {
      throw new java.io.EOFException();
      //return 0;  // send a generously infinite supply of zero-bits
    }
  }

  public void close() throws IOException {
    source.close();
  }
  
  public boolean informative() {
    return more;
  }

}
