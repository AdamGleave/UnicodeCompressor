/* Automated copy from build process */
/* $Id: ZeroMassException.java,v 1.4 2016/03/02 02:15:45 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

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
  public ZeroMassException(String s, Exception e) { super(s,e); }

}
