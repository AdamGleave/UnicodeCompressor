/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

/** A parent class of probability distributions defined by a
  * probability density function.
  * Subclasses must implement <code>density(x)</code> and
  * <code>logDensity(x)</code>, which are called by <code>p(x)</code>
  * and <code>logp(x)</code> (respectively).
  * Methods <code>mass(x)</code> and <code>logMass(x)</code> return zero
  * and negative infinity (respectively) for all <var>x</var>.
  * @see SimpleMass */
public abstract class SimpleDensity<X> extends Distribution<X> {
  
  /** Returns the probability mass at a given point (always zero).
    * @return 0 */
  public double mass(X x) {
    return 0.0;
  }
  
  /** Returns the log probability mass at a given point (always -∞).
    * @return -∞ */
  public double logMass(X x) {
    return Double.NEGATIVE_INFINITY;
  }

  /** Returns the probability density at a given point,
    * via <code>density(x)</code>.
    * @see #density(Object) */
  public double p(X x) {
    return density(x);
  }
  
  /** Returns the log probability density at a given point,
    * via <code>logDensity(x)</code>.
    * @see #logDensity(Object) */
  public double logp(X x) {
    return logDensity(x);
  }

  /** Returns the total mass of elements in the given collection
    * (always zero).
    * @return 0 */
  public double totalMass(Iterable<X> col) {
    return 0.0;
  }
  
  /** Returns the log mass at a given point, assuming a collection
    * of excluded points.
    * For simple densities, this method always returns zero.
    * @return 0 */
  public double massWithout(X x, Iterable<X> omit) {
    return 0.0;
  }
  
  /** Returns the log mass at a given point, assuming a collection
    * of excluded points.
    * For simple densities, this method always returns -∞.
    * @return -∞ */
  public double logMassWithout(X x, Iterable<X> omit) {
    return Double.NEGATIVE_INFINITY;
  }

}
