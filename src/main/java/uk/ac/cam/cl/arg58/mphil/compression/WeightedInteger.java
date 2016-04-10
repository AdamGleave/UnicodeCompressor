/* Automated copy from build process */
/* $Id: DiscreteInteger.java,v 1.8 2013/01/03 02:06:15 chris Exp $ */
package uk.ac.cam.cl.arg58.mphil.compression;

import uk.ac.cam.eng.ml.tcs27.compression.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

/** A discrete distribution over a contiguous range of integers,
  * implemented using arrays.
  * This implementation is more restrictive, but faster than the
  * <code>Discrete</code> class.  Probability mass is stored in
  * an array, so look-ups are fast.
  * The coding support in this class is inherited from
  * {@code SimpleMass}, and should work fine.
  * @see Discrete
  * @see UniformInteger */
public class WeightedInteger extends SimpleMass<Integer>
                             implements Iterable<Integer> {

  /** Lowermost integer (inclusive). */
  public int lo = 0;
  /** Uppermost integer (inclusive). */
  public int hi = 0;

  public int[] weights;
  public int n;

  /** Constructs a new WeightedInteger distribution.
    * @param start first integer
    * @param weights array of weights */
  public WeightedInteger(int start, int[] weights) {
    this.weights = weights;
    for (int i=0; i<weights.length; i++) {
      n += weights[i];
    }
    this.lo = start;
    this.hi = start + weights.length - 1;
  }

  public String toString() {
    return "WeightedInteger("+lo+".."+hi+","+Arrays.toString(weights)+")";
  }
  
  /** Returns an iterator over the elements of the domain of
    * this distribution. */
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      private int i = lo;
      public boolean hasNext() {
        return (i <= hi);
      }
      public Integer next() {
        return i++;
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public boolean isFinite() {
    return true;
  }

  @Override
  public Integer sample(Random rnd) {
    int i = rnd.nextInt(n);
    long sum = 0;
    while (sum < i) {
      sum += weights[i];
      i++;
    }
    return lo + (i - 1);
  }

  @Override
  public double mass(Integer i) {
    int idx = i - lo;
    if (idx >= 0 && idx <= (hi-lo)) {
      return (double)weights[idx] / n;
    } else {
     return 0.0;
    }
  }
  
  @Override
  public double logMass(Integer i) {
    int idx = i - lo;
    if (idx >= 0 && idx <= (hi-lo)) {
      return Math.log(weights[idx]) - Math.log(n);
    } else {
     return Double.NEGATIVE_INFINITY;
    }
  }

  @Override
  public long discreteMass(Integer i) {
    int idx = i - lo;
    if (idx <= 0 && idx <= (hi-lo)) {
      return weights[idx];
    } else {
      return 0;
    }
  }

  @Override
  public long discreteTotalMass() {
    return n;
  }
}
