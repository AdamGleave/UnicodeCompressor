/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** A parent class of probability distributions defined by a
  * probability mass function.
  * Subclasses must implement <code>mass(x)</code> and
  * <code>logMass(x)</code>, which are called by <code>p(x)</code>
  * and <code>logp(x)</code> (respectively), and used by
  * <code>density(x)</code> and <code>logDensity(x)</code>
  * to determine whether the density at point <var>x</var> is
  * infinite or zero.
  * @see SimpleDensity */
public abstract class SimpleMass<X> extends Distribution<X> {
  
  /** Returns the probability density at point <var>x</var> (0 or +∞).
    * @return 0 if mass(x) is zero, +∞ otherwise */
  public double density(X x) {
    if (mass(x) <= 0.0) {
      return 0.0;
    } else {
      return Double.POSITIVE_INFINITY;
    }
  }
  
  /** Returns the log probability density at point <var>x</var> (-∞ or +∞).
    * @return -∞ if mass(x) is zero, +∞ otherwise */
  public double logDensity(X x) {
    if (mass(x) <= 0.0) {
      return Double.NEGATIVE_INFINITY;
    } else {
      return Double.POSITIVE_INFINITY;
    }
  }

  /** Returns the probability mass of element <var>x</var>
    * via <code>mass(x)</code>.
    * @see #mass(Object) */
  public double p(X x) {
    return mass(x);
  }
  
  /** Returns the log probability mass of element <var>x</var>
    * via <code>logMass(x)</code>.
    * @see #logMass(Object) */
  public double logp(X x) {
    return logMass(x);
  }

}
