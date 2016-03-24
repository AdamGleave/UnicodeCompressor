/* Automated copy from build process */
/* $Id: OutputStreamASCIIBitWriter.java,v 1.1 2016/02/28 19:07:49 chris Exp $ */

import java.io.OutputStream;
import java.io.IOException;

/** A BitWriter that writes ASCII symbols 0 and 1 to a given OutputStream.
  * <p>Data is written to the OutputStream one character at a time.</p> */
public class OutputStreamASCIIBitWriter implements BitWriter {

  /** Output receiver. */
  private OutputStream os = null;

  /** Constructs a new ASCIIBitWriter which writes to the designated
    * OutputStream. */
  public OutputStreamASCIIBitWriter(OutputStream os) {
    this.os = os;
  }

  public void writeBit(byte bit) throws IOException {
    if (bit == 0x00) {
      os.write((int) '0');
    } else {
      os.write((int) '1');
    }
  }

  public void close() throws IOException {
    os.flush();
    os.close();
  }

}
