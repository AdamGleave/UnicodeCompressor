package compression.tcs27.mlg.eng.cam.ac.uk;/* Automated copy from build process */
/* $Id: ZeroMassException.java,v 1.3 2012/10/12 09:09:41 chris Exp $ */

/** An exception thrown by events of zero probability mass.
  * This exception is thrown, for example, when attempting
  * to encode a symbol with zero probability mass via the
  * <code>Codable</code> interface.
  * It can also be thrown when conditioning on a zero mass
  * event, e.g. by calling <code>learn(x)</code> when
  * <code>mass(x) == 0</code>. */
public class ZeroMassException extends RuntimeException {

  public ZeroMassException() { super(); }
  public ZeroMassException(String s) { super(s); }
  public ZeroMassException(Exception e) { super(e); }

}
