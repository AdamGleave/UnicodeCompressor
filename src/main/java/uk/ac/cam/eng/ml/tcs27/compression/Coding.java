/* Automated copy from build process */
/* $Id: Coding.java,v 1.9 2015/08/11 11:28:16 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/** A collection of general utility methods for compression coding. */
public class Coding {

  /** Creates a map of integer masses summing to a given budget,
    * allocated in proportion of probability mass.
    * All elements are guaranteed an allocation of positive mass,
    * even when their mass is zero.  This guarantee guards against
    * zero mass events at negligible cost.  (Distributions for which
    * this policy would be problematic would need a different
    * discretization method anyway.)
    * @param mass probability mass
    * @param set an iterable collection of elements (whose mass sums to 1)
    * @param budget total range of integers to be allocated */
  public static <X> HashMap<X,Long> getDiscreteMass(Mass<X> mass,
                                                    Iterable<X> set,
                                                    long budget) {
    HashMap<X,Long> map = new HashMap<X,Long>();
    int n = 0; // number of elements
    long sum = 0L; // total allocated mass
    for (X x : set) {
      final double m = mass.mass(x);
      long mb = (long) (m * budget);
      // this deals with zero / near-zero mass elements:
      if (mb <= 0L) { mb = 1L; }
      map.put(x, mb);
      sum += mb;
      n++;
    }
    // deal with left-over or overspent budget:
    long diff = budget - sum;
    if (diff > 0L) {
      // underspent budget
      int delta = (diff > n) ? (int) (diff / n) : 1;
      for (Map.Entry<X,Long> e : map.entrySet()) {
        e.setValue(e.getValue()+delta);
        diff-=delta;
        if (diff==0L) { break; }
        else
        if (diff <= n) { delta = 1; }
      }
    } else
    if (diff < 0L) {
      // overspent budget
      int delta = (diff < -n) ? (int) - (diff / n) : 1;
      for (Map.Entry<X,Long> e : map.entrySet()) {
        Long v = e.getValue();
        if (v > 1L) {
          e.setValue(v-delta);
          diff+=delta;
        }
        if (diff==0L) { break; }
        else
        if (diff >= -n) { delta = 1; }
      }
    }
    /* // PEAK ADJUSTMENT METHOD
    long newpeak = budget - sum + map.get(mode);
    if (newpeak > 0L) {
      map.put(mode, newpeak);
    } else {
      throw new RuntimeException("discretization failure: overspent "+(sum-budget));
    }
    */
    return map;
  }
  
  
  /** Adds discretized integer masses to an existing map.
    * All elements are guaranteed an allocation of positive mass,
    * even when their mass is zero.  This guarantee guards against
    * zero mass events at negligible cost.  (Distributions for which
    * this policy would be problematic would need a different
    * discretization method anyway.)
    * @param map map from elements to integer mass
    * @param mass probability mass
    * @param set an iterable collection of elements (whose mass sums to 1)
    * @param budget total integer mass to be added to the map
    * @return remaining budget (if negative, budget was overspent) */
  public static <X> long addDiscreteMass(Map<X,Long> map,
                                         Mass<X> mass,
                                         Iterable<X> set,
                                         long budget) {
    long sum = 0L;
    for (X x : set) {
      final double m = mass.mass(x);
      // Here mb stores the additional mass
      long mb = (long) (m * (double) budget);
      sum += mb;
      // add existing mass
      Long e = map.get(x);
      if (e != null) {
        mb += e;
      }
      // Here mb stores the updated (total) mass
      // deal with zero / near-zero mass elements:
      if (mb == 0L) {
        mb = 1L;
      }
      map.put(x, mb);
    }
    return budget - sum;
  }
  
  
  /** Encodes a value using a given table of discretized point masses.
    * The masses must sum to the given encoder's current coding range.
    * The elements are traversed in the given order.
    * @param x value to be encoded
    * @param dmass discretized mass for each element
    * @param order an iterable over all elements
    * @param ec the encoder
    * @throws IllegalArgumentException if the value to be encoded is
    *         not defined in the map */
  public static <X> void encode(X x, Map<X,Long> dmass,
                                Iterable<X> order, Encoder ec) {
    // now encode
    long sum = 0L;
    for (X y : order) {
      long m = dmass.get(y);
      if (x.equals(y)) {
        ec.storeRegion(sum,sum+m);
        return;
      }
      sum += m;
    }
    throw new IllegalArgumentException("element "+x+" is not defined");
  }
  
  /** Decodes a value using a given table of discretized point masses.
    * The masses must sum to the given decoder's current coding range.
    * The elements are traversed in the given order.
    * @param dmass discretized mass for each element
    * @param order an iterable over all elements
    * @param dc the decoder
    * @return the decoded element */
  public static <X> X decode(Map<X,Long> dmass,
                             Iterable<X> order, Decoder dc) {
    // now decode
    long r = dc.getTarget();
    long sum = 0L;
    for (X x : order) {
      long m = dmass.get(x);
      if (r >= sum && r < sum+m) {
        dc.loadRegion(sum,sum+m);
        return x;
      }
      sum += m;
    }
    // if we get here, something clearly went wrong
    throw new IllegalStateException("targeting unused coding range");
  }
  
  /** Encodes a value given a probability mass function and ordering.
    * The elements are traversed in the given order.
    * @param x value to be encoded
    * @param mass probability mass function
    * @param order an iterable over all elements
    * @param ec the encoder
    * @throws IllegalArgumentException if the value to be encoded is
    *         not defined in the map */
  public static <X> void encode(X x, Mass<X> mass,
                                Iterable<X> order, Encoder ec) {
    long budget = ec.getRange();
    Map<X,Long> dmass = getDiscreteMass(mass,order,budget);
    encode(x,dmass,order,ec);
  }
  
  /** Decodes a value given a probability mass function and ordering.
    * The elements are traversed in the given order.
    * @param mass probability mass function
    * @param order an iterable over all elements
    * @param dc the decoder
    * @return the decoded element */
  public static <X> X decode(Mass<X> mass,
                             Iterable<X> order, Decoder dc) {
    long budget = dc.getRange();
    Map<X,Long> dmass = getDiscreteMass(mass,order,budget);
    return decode(dmass,order,dc);
  }

  /** Encodes an integer using an array of discretized point masses.
    * The masses must sum to the given encoder's current coding range.
    * The integers are traversed from 0 to the length of the
    * given array plus one.
    * @param k integer to be encoded
    * @param dmass discretized mass for each integer from zero onwards
    * @param ec the encoder */
  public static void encode(int k, long[] dmass, Encoder ec) {
    long sum = 0L;
    for (int j=0; j<k; j++) {
      sum += dmass[j];
    }
    ec.storeRegion(sum,sum+dmass[k]);
  }
  
  /** Decodes an integer using an array of discretized point masses.
    * The masses must sum to the given decoder's current coding range.
    * The integers are traversed from 0 to the length of the
    * given array plus one.
    * @param dmass discretized mass for each integer from zero onwards
    * @param dc the decoder
    * @return the decoded integer
    * @throws IllegalStateException if the array doesn't sum to
    *         the budget of the decoder */
  public static int decode(long[] dmass, Decoder dc) {
    long sum = 0L;
    long target = dc.getTarget();
    for (int j=0; j<dmass.length; j++) {
      long m  = dmass[j];
      long sm = sum+m;
      if (target >= sum && target < sm) {
        dc.loadRegion(sum,sm);
        return j;
      }
      sum = sm;
    }
    throw new IllegalStateException("targeting unused coding range");
  }
  

}

