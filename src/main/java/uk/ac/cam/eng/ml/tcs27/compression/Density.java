/* Automated copy from build process */
/* $Id: Density.java,v 1.1 2012/10/01 21:19:34 chris Exp $ */

/** The interface of probability density functions. */
public interface Density<X> {

  /** Computes the probability density at the given point.
    * Probability density is a real value between 0 and positive infinity.
    * @see #logDensity(Object) */
  public double density(X x);

  /** Computes the log probability density at the given point.
    * The log probability density is a real value between negative infinity
    * and positive infinity.
    * @see #density(Object) */
  public double logDensity(X x);

}
