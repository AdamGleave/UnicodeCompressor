/* Automated copy from build process */
/* $Id: InputStreamASCIIBitReader.java,v 1.1 2016/02/28 19:07:49 chris Exp $ */

import java.io.InputStream;
import java.io.IOException;

/** A BitReader which reads ASCII chars {0,1} from a given InputStream.
  * After the source InputStream has reached its end, this BitReader
  * throws an EOFException. */
public class InputStreamASCIIBitReader implements BitReader {
  
  /** Bit source. */
  private InputStream source = null;

  /** Indicator if the bit source is still active. */
  private boolean more = true;

  /** Bit counter. */
  long pos = 0L;

  /** Constructs a new BitReader which reads ASCII chars from given InputStream. */
  public InputStreamASCIIBitReader(InputStream is) {
    this.source = is;
  }

  public byte readBit() throws IOException {
    int next = source.read();
    if (next != -1) {
      pos++;
      if (next == (int) '0') {
        return (byte) 0;
      } else
      if (next == (int) '1') {
        return (byte) 1;
      } else {
        throw new java.io.IOException("unexpected symbol at position "+pos);
      }
    } else {
      throw new java.io.EOFException("at position "+pos);
    }
  }

  public void close() throws IOException {
    source.close();
  }
  
  public boolean informative() {
    return more;
  }

}
