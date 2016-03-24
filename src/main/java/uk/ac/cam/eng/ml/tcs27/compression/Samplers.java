/* Automated copy from build process */
/* JAVA Probability and Inference Tools
 * $Id: Samplers.java,v 1.7 2015/08/11 02:14:04 chris Exp $
 * Author: Christian Steinruecken */

import java.util.Random;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Vector;

/** A class of simple sampling methods. */
public class Samplers {
  
  /** Draws <var>n</var> samples from a given sampler and adds
    * them to a given collection.
    * @param rnd random source
    * @param s sampler
    * @param col target collection
    * @param n number of samples */
  public static <X> void sample(Random rnd, Sampler<X> s,
                                Collection<X> col, int n) {
    for (int k=0; k<n; k++) {
      col.add(s.sample(rnd));
    }
  }
  
  /** Sample from a given sampler, rejecting elements in the
    * specified collection.
    * Simple rejection sampling is used: this may loop infinitely when
    * <code>s.sample(rnd)</code> produces only elements already contained
    * in <code>omit</code>.
    * @param rnd random source
    * @param s sampler
    * @param omit collection of elements to omit */
  public static <X> X sampleWithout(Random rnd, Sampler<X> s,
                                    Collection<X> omit) {
    X x = s.sample(rnd);
    while (omit.contains(x)) {
      x = s.sample(rnd);
    }
    return x;
  }

  
  /** Samples an integer from 0 to <var>k</var>-1,
    * with probability masses given by <var>k</var>-length
    * collection <var>pp</var>.
    * Note that <var>pp</var>:s components must sum to 1.
    * @param rnd random source
    * @param pp iterable collection of probability masses
    * @return an integer between 0 (inclusive) and k (exclusive). */
  public static int sampleIndex(Random rnd, Iterable<Double> pp) {
    return sampleIndex(rnd, pp, 1.0);
  }
  
  /** Samples an integer from 0 to <var>k</var>-1,
    * with unnormalized probability masses given by <var>k</var>-length
    * collection <var>pp</var>, and total mass given by <var>m</var>.
    * Naturally, <var>pp</var>:s components should sum to <var>m</var>.
    * If they sum to <i>less</i> than <var>m</var>, rejection sampling
    * is used to still get an unbiased sample.  If the sum exceeds
    * <var>m</var>, things are definitely biased and nobody can help you.
    * @param rnd random source
    * @param pp iterable collection of probability masses
    * @param m total mass (or upper bound of total mass)
    * @return an integer between 0 (inclusive) and k (exclusive)
    * @see #sampleIndex(Random, double[], double) */
  public static int sampleIndex(Random rnd, Iterable<Double> pp, double m) {
    double u = rnd.nextDouble();
    if (m != 1.0) { u = m * u; }
    double mass = 0.0;
    int k = 0;
    for (Double p : pp) {
      mass += p;
      if (mass >= u) {
        return k;
      }
      k++;
    }
    if (mass == 0.0) {
      throw new ZeroMassException();
    }
    // We should never get here, if the pp really sum to m.
    // But if we do get here, we'll resample with the correct total.
    return sampleIndex(rnd,pp,mass);
  }
  
  /** Samples an integer from 0 to <var>k</var>-1,
    * with unnormalized probability masses given by <var>k</var>-length
    * array <var>pp</var>, and total mass given by <var>m</var>.
    * Naturally, <var>pp</var>:s components should sum to <var>m</var>.
    * If they sum to <i>less</i> than <var>m</var>, rejection sampling
    * is used to still get an unbiased sample.  If the sum exceeds
    * <var>m</var>, things are definitely biased and nobody can help you.
    * @param rnd random source
    * @param pp iterable collection of probability masses
    * @param m total mass (or upper bound of total mass)
    * @return an integer in [0, k-1]. 
    * @see #sampleIndex(Random, Iterable, double) */
  public static int sampleIndex(Random rnd, double[] pp, double m) {
    double u = rnd.nextDouble();
    if (m != 1.0) { u = m * u; }
    double mass = 0.0;
    int k = 0;
    for (double p : pp) {
      mass += p;
      if (mass >= u) {
        return k;
      }
      k++;
    }
    if (mass == 0.0) {
      throw new ZeroMassException();
    }
    // We should never get here, if the pp[k] really sum to m.
    // But if we do get here, we'll resample with the correct total.
    return sampleIndex(rnd,pp,mass);
  }
    
  /** Reservoir-samples an element, uniformly at random.
    * Reservoir sampling has linear time complexity, but avoids
    * the need to compute the collection's size in advance.
    * If the specified collection is empty, null is returned.
    * @param rnd random source
    * @param col iterable collection of elements
    * @return a uniformly random element */
  public static <X> X rsvsample(Random rnd, Iterable<X> col) {
    X e = null;
    int k = 0;   /* elements seen so far */
    for (X x : col) {
      k++;
      e = (e == null) ? x : (rnd.nextInt(k) == 0 ? x : e);
    }
    return e;
  }
  
  /** Samples an element from a Collection, uniformly at random.
    * Uses the collection's iterator, unless the collection is
    * a list supporting random access, in which case a call to
    * List.get() is made instead.
    * If the specified collection is empty, null is returned.
    * @see java.util.RandomAccess
    * @param rnd random source
    * @param col collection of elements
    * @return a random element, or null */
  public static <X> X uniform(Random rnd, Collection<X> col) {
    int n = col.size();
    if (n > 0) {
      int k = rnd.nextInt(n);  /* selected element */
      if (col instanceof java.util.List &&
          col instanceof java.util.RandomAccess) {
        return ((List<X>) col).get(k);
      } else {
        X e = null;
        Iterator<X> it = col.iterator();
        for (int j=0; j < k; j++) {
          e = it.next();
        }
        return e;
      }
    } else {
      return null;
    }
  }
  
  
  /** Samples an element from a map of elements to probability masses.
    * @param rnd random source
    * @param map map of elements to probability masses
    * @param m total mass (or upper bound of total mass)
    * @return a random element */
  public static <X> X sample(Random rnd, Map<X,Double> map, double m) {
    double u = rnd.nextDouble();
    if (m != 1.0) { u = m * u; }
    double mass = 0.0;
    for (Map.Entry<X,Double> e : map.entrySet()) {
      mass += e.getValue();
      if (mass >= u) {
        return e.getKey();
      }
    }
    if (mass == 0.0) {
      throw new ZeroMassException();
    }
    // We should never get here, if the masses in the map really sum to m.
    // But if we do get here, we'll resample with the correct total.
    return sample(rnd,map,mass);
  }
  
  /** Samples an element from a map of elements to probability masses.
    * The probability masses in the map are assumed to sum to one.
    * @param rnd random source
    * @param map map of elements to probability masses
    * @return a random element */
  public static <X> X sample(Random rnd, Map<X,Double> map) {
    return sample(rnd, map, 1.0);
  }


  /** Samples a uniformly distributed real number between 0 and 1.
    * @param rnd random source */
  public static double uniform(Random rnd) {
    return rnd.nextDouble();
  }
  
  /** Samples a uniformly distributed real number between 0 and
    * a given real number.
    * @param rnd random source
    * @param b bound (may be positive or negative) */
  public static double uniform(Random rnd, double b) {
    return rnd.nextDouble()*b;
  }

  /** Samples a uniformly distributed real number between two
    * given real numbers.
    * @param rnd random source
    * @param a lower margin of interval (inclusive)
    * @param b upper margin of interval (exclusive) */
  public static double uniform(Random rnd, double a, double b) {
    return a+(rnd.nextDouble()*(b-a));
  }

  /** Returns the result of a fair coin flip. */
  public static boolean flip(Random rnd) {
    return rnd.nextBoolean();
  }
  
  /** Returns the result of a coin flip with given bias.
    * P(true) = p, P(false) = 1-p. */
  public static boolean flip(Random rnd, double p) {
    return uniform(rnd) < p;
  }

  /** Returns the element of smallest probability mass from a given
    * iterable collection.
    * If several elements qualify, the one which occurs first in
    * the iterable collection is returned.
    * @param d distribution
    * @param set set of elements to consider
    * @return the first element of least mass */
  public static <X> X leastMass(Mass<X> d, Iterable<X> set) {
    X least = null;
    double lmass = Double.POSITIVE_INFINITY;
    for (X x : set) {
      double m = d.mass(x);
      if (m < lmass) {
        lmass = m;
        least = x;
      }
    }
    return least;
  }
  
  /** Returns a vector of elements whose probability probability mass
    * is smallest under a given probability mass function.
    * @param d distribution
    * @param set set of elements to consider
    * @return subset of elements of least mass, in order of occurrence */
  public static <X> Vector<X> leastMassAll(Mass<X> d, Iterable<X> set) {
    Vector<X> least = null;
    double lmass = Double.POSITIVE_INFINITY;
    for (X x : set) {
      double m = d.mass(x);
      if (m < lmass) {
        lmass = m;
        least = new Vector<X>();
        least.add(x);
      } else
      if (m == lmass) {
        least.add(x);
      }
    }
    return least;
  }
  
  /** Returns an element of smallest probability mass,
    * randomly chosen from a given iterable collection.
    * If several elements qualify, one is chosen uniformly at random.
    * @param d distribution
    * @param set set of elements to consider
    * @param rnd random source
    * @return a random element of least mass */
  public static <X> X leastMass(Mass<X> d, Iterable<X> set, Random rnd) {
    Vector<X> least = leastMassAll(d,set);
    int size = least.size();
    if (size > 1) {
      return least.get(rnd.nextInt(size));
    } else {
      return least.get(0);
    }
  }
  
  /** Returns the element of highest probability mass from a given
    * iterable collection.
    * If several elements qualify, the one which occurs first in
    * the iterable collection is returned.
    * @param d distribution
    * @param set set of elements to consider
    * @return the first element of highest mass */
  public static <X> X mostMass(Mass<X> d, Iterable<X> set) {
    X most = null;
    double hmass = Double.NEGATIVE_INFINITY;
    for (X x : set) {
      double m = d.mass(x);
      if (m > hmass) {
        hmass = m;
        most = x;
      }
    }
    return most;
  }
  
  /** Returns a vector of elements whose probability probability mass
    * is highest under a given probability mass function.
    * @param d distribution
    * @param set set of elements to consider
    * @return subset of elements of highest mass, in order of occurrence */
  public static <X> Vector<X> mostMassAll(Mass<X> d, Iterable<X> set) {
    Vector<X> most = null;
    double hmass = Double.NEGATIVE_INFINITY;
    for (X x : set) {
      double m = d.mass(x);
      if (m > hmass) {
        hmass = m;
        most = new Vector<X>();
        most.add(x);
      } else
      if (m == hmass) {
        most.add(x);
      }
    }
    return most;
  }
  
  /** Returns an element of highest probability mass,
    * randomly chosen from a given iterable collection.
    * If several elements qualify, one is chosen uniformly at random.
    * @param d distribution
    * @param set set of elements to consider
    * @param rnd random source
    * @return a random element of highest mass */
  public static <X> X mostMass(Mass<X> d, Iterable<X> set, Random rnd) {
    Vector<X> most = mostMassAll(d,set);
    int size = most.size();
    if (size > 1) {
      return most.get(rnd.nextInt(size));
    } else {
      return most.get(0);
    }
  }

}
