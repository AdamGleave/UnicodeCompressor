/* Automated copy from build process */
/* JAVA Probability and Inference Tools
 * $Id: Distribution.java,v 1.46 2015/08/09 23:28:15 chris Exp $
 * Author: Christian Steinruecken */

import java.util.Random;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.io.PrintStream;

/** The parent class of all probability distributions. */
public abstract class Distribution<X> implements Sampler<X>,
                                                 Codable<X>,
                                                 Density<X>,
                                                 Mass<X> {
  
  /** Computes the probability mass of the supplied element.
    * The probability mass is a real value between 0 and 1 (inclusive).
    * <br><b>Note:</b> Do not confuse this with probability density.
    * @see #density(Object) */
  public abstract double mass(X x);
  
  /** Computes the log probability mass of the supplied element.
    * The log probability mass is a real value between negative infinity
    * and zero (inclusive).
    * <br><b>Note:</b> Do not confuse this with log probability density.
    * @see #logDensity(Object) */
  public abstract double logMass(X x);
  
  /** Computes the probability density at the given point.
    * Probability density is a real value between 0 and positive infinity.
    * <br><b>Note:</b> Do not confuse this with probability mass.
    * @see #mass(Object) */
  public abstract double density(X x);
  
  /** Computes the log probability density at the given point.
    * The log probability density is a real value between negative infinity
    * and positive infinity.
    * <br><b>Note:</b> Do not confuse this with log probability mass.
    * @see #logMass(Object) */
  public abstract double logDensity(X x); 

  /** Returns a random sample from this distribution. */
  public abstract X sample(Random rnd);

  /** Computes the probability mass or density of the supplied element. */
  public abstract double p(X x);

  /** Computes the log probability mass or density of the supplied
    * element. */
  public abstract double logp(X x);

  /** Returns the total mass of elements in <var>col</var>.
    * This equals the sum of all individual masses of each value
    * in the collection.  If <var>col</var> is a set, the 
    * the return value should always be smaller or equal to 1.
    * If <var>col</var> is empty, 0 is returned.
    * If <var>col</var> contains repeated elements, the return
    * value may exceed 1. */
  public double totalMass(Iterable<X> col) {
    double mass = 0.0;
    for (X x : col) {
      mass += mass(x);
    }
    return mass;
  }
  
  /** Returns an integer proportional to the
    * probability mass of the given element.
    * Use {@code discreteTotalMass()} to get the normalising
    * constant.
    * @throws UnsupportedOperationException if unimplemented
    * @see #discreteTotalMass() */
  public long discreteMass(X x) {
    throw new UnsupportedOperationException();
  }

  /** Returns an integer representating the total
    * probability mass of this distribution.
    * This is the normalising constant for integer
    * probability masses computed via {@code discreteMass(X)}.
    * @throws UnsupportedOperationException if unimplemented
    * @see #discreteMass(Object) */
  public long discreteTotalMass() {
    throw new UnsupportedOperationException();
  }

  /** Returns an integer proportional to the total
    * probability mass of elements in the given collection.
    * This equals the sum of all individual masses of each value
    * in the collection.  If <var>col</var> is a set, the 
    * the return value should is an integer between 0 and the
    * normalising constant given by {@code discreteTotalMass()},
    * inclusive. If <var>col</var> is empty, 0 is returned.
    * If <var>col</var> contains repeated elements, the return
    * value may exceed the normalising constant.
    * @see #discreteMass(Object) */
  public long discreteTotalMass(Iterable<X> col) {
    long mass = 0;
    for (X x : col) {
      mass += discreteMass(x);
    }
    return mass;
  }
  
  /** Returns an integer proportional to the total
    * probability mass of the distribution excluding
    * elements in the specified collection.
    * If the collection is empty, the returned value equals
    * the one given by {@code discreteTotalMass()}.
    * If the collection has repeated elements, an incorrect
    * value may be obtained.
    * @param excl set of excluded elements
    * @see #discreteTotalMass() */
  public long discreteTotalMassWithout(Iterable<X> excl) {
    return discreteTotalMass() - discreteTotalMass(excl);
  }
  
  /** Computes the rescaled mass of the supplied element, after
    * exclusion of a specified set of elements.
    * Returns mass(x) / (1 - total excluded mass) for elements
    * which are not excluded, and 0.0 otherwise.
    * @param x the element
    * @param omit a collection excluded set
    * @see #mass(Object)
    * @see #totalMass(Iterable)
    * @see #logMassWithout(Object,Collection) */
  public double massWithout(X x, Collection<X> omit) {
    if (omit.contains(x)) {
      return 0.0;
    } else {
      return mass(x) / (1.0 - totalMass(omit));
    }
  }
  
  /** Computes the log mass of the supplied element, rescaled
    * to account for exclusion of a specified set of elements.
    * Returns (log mass(x) - log (1 - total excluded mass))
    * for elements which are not excluded, and -∞ otherwise.
    * @param x the element
    * @param omit a collection of excluded elements
    * @see #logMass(Object)
    * @see #massWithout(Object,Collection) */
  public double logMassWithout(X x, Collection<X> omit) {
    if (omit.contains(x)) {
      return Double.NEGATIVE_INFINITY;
    } else {
      return logMass(x) - Math.log(1.0 - totalMass(omit));
    }
  }
  

  /* We're including the Codable interface. */

  @Override
  @SuppressWarnings("unchecked")
  public void encode(X x, Encoder ec) {
    if (isFinite() && isIterable()) {
      // get budget + mass allocation
      long budget = ec.getRange();
      Iterable<X> set = (Iterable<X>) this;
      HashMap<X,Long> mass = Coding.getDiscreteMass(this,set,budget);
      Coding.encode(x, mass, set, ec);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public void encode(X x, Collection<X> omit, Encoder ec) {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public X decode(Decoder dc) {
    if (isFinite() && isIterable()) {
      // get budget + mass allocation
      long budget = dc.getRange();
      Iterable<X> set = (Iterable<X>) this;
      HashMap<X,Long> mass = Coding.getDiscreteMass(this,set,budget);
      return Coding.decode(mass, set, dc);
    } else {
      throw new UnsupportedOperationException();
    }
  }
  
  @Override
  public X decode(Collection<X> omit, Decoder dc) {
    throw new UnsupportedOperationException();
  }


  /** Returns the mode of this distribution. */
  //public abstract X mode();

  /** Returns the mean of this distribution. */
  // Note: the mean is well defined only on vector spaces
  //public abstract Object mean();
  
  /** Returns the variance of this distribution. */
  // Note: variance is well defined only on rings
  //       (≈ vector space + multiplication)
  //public abstract Object variance();

  /** Returns the entropy of this distribution.
    * Entropy is defined as the expected information content
    * of a random variable distributed with this distribution.
    * @throws UnsupportedOperationException if this is unimplemented. */
  public double entropy() {
    throw new UnsupportedOperationException();
    /* (This might be well-defined for discrete distributions only.) */
  }

  /** Update this distribution to its posterior after observing
    * datapoint <var>x</var>.  This is experimental and not recommended,
    * since this is kind of call-back can never work in general.
    * This should be replaced by a proper framework for hierarchical
    * Bayesian learning.
    * @throws UnsupportedOperationException if this is unimplemented. */
  public void learn(X x) {
    throw new UnsupportedOperationException();
  }

  /** Returns a copy of this distribution.
    * @throws UnsupportedOperationException if this is unimplemented. */
  public Distribution<X> clone() {
    //throw new CloneNotSupportedException();
    throw new UnsupportedOperationException();
  }

  /** Returns if this distribution is defined on a finite number
    * of elements.  Note that discrete distributions can be infinite
    * (for example the negative binomial distribution), and that
    * continuous-valued distributions can be finite (when defined on
    * a finite number of elements of the space). */
  public boolean isFinite() {
    throw new UnsupportedOperationException();
  }

  /** Returns if this distribution implements Iterable.
    * The iterator should iterate over the distribution's elements.
    * @return true if and only if {@code (this instanceof Iterable)}. */
  public boolean isIterable() {
    return (this instanceof Iterable);
  }


  /* Helpful tools */

  /** Sample <var>n</var> values and add them to collection <var>col</var>.
    * @see #sample(Random) */
  public void sample(Random rnd, int n, Collection<X> col) {
    for (int k=0; k<n; k++) {
      col.add(sample(rnd));
    }
  }
  
  /** Sample from this distribution, excluding elements in <var>omit</var>.
    * <p><b>Note:</b> The implementation given in <code>Distribution</code>
    * uses simplistic rejection sampling, which may loop infinitely when
    * <code>sample(rnd)</code> produces only elements already contained
    * in <code>omit</code>.</p>
    * @see #sample(Random) */
  public X sampleWithout(Collection<X> omit, Random rnd) {
    X x = sample(rnd);
    while (omit.contains(x)) {
      x = sample(rnd);
    }
    return x;
  }

 
  /** Prints debugging information about this distribution to
    * a designated PrintStream (for example System.out). */
  @SuppressWarnings("unchecked")
  public void printDebugInfo(PrintStream out) {
    final String pre = "| ";
    out.println(pre+" Dist: "+toString());
    boolean fin = isFinite();
    boolean itr = isIterable();
    if (fin) {
      if (itr) {
        Iterable<X> set = (Iterable<X>) this;
        double sum = 0.0;
        int cnt = 0;
        for (X x : set) {
          sum += mass(x);
          cnt++;
        }
        out.println(pre+" Domain: "+cnt+" atoms");
        out.println(pre+" Atomic mass sums to: "+sum);
      } else {
        out.println(pre+" Domain: finite, non-iterable");
      }
    } else {
      out.println(pre+" Domain: infinite, "+(itr ? "" : "non-")+"iterable");
    }
  }

}
