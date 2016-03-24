/* Automated copy from build process */
/* $Id: Mass.java,v 1.1 2012/10/01 21:19:34 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** The interface of probability mass functions. */
public interface Mass<X> {

  /** Computes the probability mass of the supplied element.
    * The probability mass is a real value between 0 and 1 (inclusive).
    * @see #logMass(Object) */
  public double mass(X x);

  /** Computes the log probability mass of the supplied element.
    * The log probability mass is a real value between negative infinity
    * and zero (inclusive).
    * @see #mass(Object) */
  public double logMass(X x);

}
