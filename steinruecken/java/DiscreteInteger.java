/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Random;
import java.util.Arrays;
import java.util.Iterator;

/** A discrete distribution over a contiguous range of integers,
  * implemented using arrays.
  * This implementation is more restrictive, but faster than the
  * <code>Discrete</code> class.  Probability mass is stored in
  * an array, so look-ups are fast.
  * The coding support in this class is inherited from
  * {@code SimpleMass}, and should work fine.
  * @see Discrete
  * @see UniformInteger */
public class DiscreteInteger extends SimpleMass<Integer>
                             implements Iterable<Integer> {

  /** Lowermost integer (inclusive). */
  public int lo = 0;
  /** Uppermost integer (inclusive). */
  public int hi = 0;

  public double[] prob;

  /** Constructs a new DiscreteInteger distribution.
    * @param start first integer
    * @param prob array of probability masses */
  public DiscreteInteger(int start, double[] prob) {
    this.prob = prob;
    this.lo = start;
    this.hi = start + prob.length - 1;
  }
  
  /** Constructs a new DiscreteInteger distribution by copying
    * an existing distribution.
    * @param dist existing integer mass function
    * @param start first integer (inclusive)
    * @param end last integer (inclusive) */
  public DiscreteInteger(Mass<Integer> dist, int start, int end) {
    this.prob = new double[end-start+1];
    this.lo = start;
    this.hi = end;
    for (int k=start; k<=end; k++) {
      prob[k-start] = dist.mass(k);
    }
  }

  public String toString() {
    double sum = 0.0;
    for (int k=0; k<prob.length; k++) {
      sum += prob[k];
    }
    return "DiscreteInteger("+lo+".."+hi+","+Arrays.toString(prob)+",sum="+sum+")";
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
    return lo + Samplers.sampleIndex(rnd, prob, 1.0);
  }

  @Override
  public double mass(Integer i) {
    int idx = i - lo;
    if (idx >= 0 && idx <= (hi-lo)) {
      return prob[idx];
    } else {
     return 0.0;
    }
  }
  
  @Override
  public double logMass(Integer i) {
    int idx = i - lo;
    if (idx >= 0 && idx <= (hi-lo)) {
      return Math.log(prob[idx]);
    } else {
     return Double.NEGATIVE_INFINITY;
    }
  }

}
