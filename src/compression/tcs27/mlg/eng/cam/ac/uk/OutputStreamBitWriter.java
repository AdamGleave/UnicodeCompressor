package compression.tcs27.mlg.eng.cam.ac.uk;/* Automated copy from build process */
/* $Id: OutputStreamBitWriter.java,v 1.3 2013/04/29 17:26:53 chris Exp $ */

import java.io.OutputStream;
import java.io.IOException;

/** A BitWriter which writes bits to a given OutputStream.
  * <p>Data is written to the OutputStream in byte-sized packets.
  * When <code>close()</code> is called, any remaining bits will be flushed
  * to the OutputStream, padding up the lower-order bits of the final
  * output byte with zeros.</p> */
public class OutputStreamBitWriter implements BitWriter {

  /** Output receiver. */
  private OutputStream os = null;

  /** Current data byte. */
  private int data = 0x00;

  /** Current bit mask. */
  private int mask = 0x80;  // bit mask

  /** Constructs a new BitWriter which writes to the designated
    * OutputStream. */
  public OutputStreamBitWriter(OutputStream os) {
    this.os = os;
  }

  public void writeBit(byte bit) throws IOException {
    if (bit == 1) {
      data |= mask;  // flip the bit to a 1
    }
    mask >>= 1;
    if (mask == 0x00) {
      // flush next byte
      os.write(data);
      data = 0x00;
      mask = 0x80;
    }
  }

  public void close() throws IOException {
    if (mask != 0x80) {
      // flush out last, incomplete byte (lower-order bits are zero)
      os.write(data);
    }
    os.flush();
    os.close();
  }

}
