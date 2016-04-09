/* Automated copy from build process */
/* $Id: UniformInteger.java,v 1.11 2014/05/14 15:46:38 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.*;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** A uniform distribution over a finite range of integers.
  * This implementation is faster and smaller than the more generic
  * {@code Discrete}.
  * This class uses a small, constant amount of memory per instance,
  * whereas {@code Discrete} needs memory linear to the number of
  * elements.
  * @see Discrete */
public class UniformInteger extends SimpleMass<Integer>
                            implements Externalizable, Codable<Integer>,
                                       AdaptiveCode<Integer>,
                                       Iterable<Integer> {

  int lo = Integer.MIN_VALUE;
  int hi = Integer.MAX_VALUE;

  /** Total number of elements this distribution assigns mass to. */
  private int n = 0;
  private double logp = Double.NEGATIVE_INFINITY;


  /** Constructs a new uniform distribution over integers.
    * The new distribution shares mass equally among integers
    * ranging from <var>lowest</var> to <var>highest</var> (inclusive).
    * @see Discrete#integers(int,int) */
  public UniformInteger(int lowest, int highest) {
    if (lowest > highest) {
      throw new IllegalArgumentException();
    }
    this.lo = lowest;
    this.hi = highest;
    this.n  = hi-lo+1;
    this.logp = Double.NaN; // compute only when needed
  }

  public double mass(Integer x) {
    if (x < lo || x > hi) {
      return 0.0;
    } else {
      return 1.0/n;
    }
  }
  
  public double logMass(Integer x) {
    if (x < lo || x > hi) {
      return Double.NEGATIVE_INFINITY;
    } else {
      if (Double.isNaN(logp)) {
        // compute it once and cache it
        logp = -Math.log(n);
      } 
      return logp;
    }
  }


  public void encode(Integer x, Encoder ec) {
    if (x < lo || x > hi) {
      throw new IllegalArgumentException("integer outside range ["+lo+","+hi+"]");
    }
    ec.storeRegion(x-lo,x-lo+1,n);
  }

  public Integer decode(Decoder dc) {
    long i = dc.getTarget(n);
    dc.loadRegion(i,i+1,n);
    return (int) i+lo;
  }
  
  public void encode(Integer x, Collection<Integer> omit, Encoder ec) {
    if (x < lo || x > hi) {
      throw new IllegalArgumentException("integer outside range ["+lo+","+hi+"]");
    }
    int rtotal  = 0;
    int rbefore = 0;
    for (int o : omit) {
      if (o >= lo && o <= hi) {
        // record "to be omitted" integers that are in range
        rtotal++;
        if (o < x) {
          // and count how many occur before x
          rbefore++;
        } else
        if (o == x) {
          // obviously x itself had better not be omitted
          throw new IllegalArgumentException("attempt to encode an excluded"
                                            +" integer");
        }
      }
    }
    long k = (x - lo - rbefore);
    ec.storeRegion(k,k+1,n-rtotal);
  }
  
  public Integer decode(Collection<Integer> omit, Decoder dc) {
    int rtotal  = 0;
    for (int o : omit) {
      if (o >= lo && o <= hi) {
        // count "to be omitted" integers which are in range
        rtotal++;
      }
    }
    if (rtotal == 0) {
      // fall back on cheap and simple method
      return decode(dc);
    } else {
      long target = dc.getTarget(n-rtotal);

      // copy omit into an array (unboxing as we go), then sort
      int[] sortedOmit = new int[rtotal];
      int i = 0;
      for (int e : omit) {
        if (e >= lo && e <= hi) {
          sortedOmit[i++] = e;
        }
      }
      Arrays.sort(sortedOmit);

      /* target specifies an index into the range [lo,hi], with the elements in omit removed.
         To decode val, we initialise it to lo. We then take target 'steps'. Moving from x to x+1
         costs 1 step, *unless* x+1 is in omit, in which case it is free.

         While the number of steps taken is less than the target, we compute delta, the number
         of steps before the target is hit. If taking this many steps would jump over or onto
         an omitted element (omit_delta <= delta), val is incremented 'for free' (without
         incrementing num_steps), and delta is set to omit_delta. Both num_steps and val are
         incremented by delta (the number of steps taken).

         It's possible we take target steps but end on an element that is omitted. In this case,
         we step over it, repeating the process if the next element is also omitted.
       */
      int val = lo;
      long num_steps = 0;
      i = 0;

      while (num_steps < target) {
        long delta = target - num_steps;
        if (i < sortedOmit.length) {
          // there's still omissions left
          long omit_delta = sortedOmit[i] - val;
          if (omit_delta <= delta) {
            val++;
            i++;
            delta = omit_delta;
          }
        }
        num_steps += delta;
        val += delta;
      }

      while (i < sortedOmit.length && sortedOmit[i] == val) {
        val++;
        i++;
      }

      dc.loadRegion(target,target+1,n-rtotal);
      return val;
    }
  }
  
  public long discreteMass(Integer k) {
    return (k >= lo && k <= hi) ? 1 : 0;
  }
  
  public long discreteTotalMass() {
    return n;
  }
  
  public long discreteTotalMass(Iterable<Integer> col) {
    long mass = 0;
    for (Integer k : col) {
      if (k >= lo && k <= hi) {
        mass++;
      }
    }
    return mass;
  }

  public double totalMass(Iterable<Integer> col) {
    long mass = discreteTotalMass(col);
    return (double) mass / (double) n;
  }

  /** Dummy implementation, performs no learning. */
  public void learn(Integer k) {
    
  }

  public Distribution<Integer> getPredictiveDistribution() {
    return this;
  }


  /** Returns a cloned copy of this distribution. */
  public UniformInteger clone() {
    return new UniformInteger(lo,hi);
  }

  /** Returns the number of elements on which this distribution
    * is defined. */
  public int size() {
    return n;
  }

  /** Returns if this distribution is defined over a finite set of
    * elements.  For the UniformInteger class, this is always true.
    * @return true */
  public boolean isFinite() {
    return true;
  }


  /** Returns an approximate String representation of this
    * distribution.  Descriptions may be incomplete for distributions
    * which are complicated or have a domain of many elements.
    * Elements are omitted for the sake of readability. */
  public String toString() {
    if (n > 0) {
      return "UniformInteger("+lo+".."+hi+")";
    } else {
      return "UniformInteger(<no mass>)";
    }
  }

  /** Samples from this distribution. */
  public Integer sample(Random rnd) {
    return rnd.nextInt(n)+lo;
  }
  
  /** Samples from this distribution, omitting elements in <var>omit</var>.
    * Note: if <var>omit</var> contains duplicate values, this method
    * may fail in unexpected ways. */
  public Integer sampleWithout(Collection<Integer> omit, Random rnd) {
    int rtotal = 0;
    for (int o : omit) {
      if (o >= lo && o <= hi) {
        // count "to be omitted" integers which are in range
        rtotal++;
      }
    }
    int i = rnd.nextInt(n-rtotal);
    if (rtotal == 0) {
      return i+lo;
    } else {
      // TODO: verify this really is correct
      int  x = lo;
      int  k = 0;
      // this could probably be done more efficiently...
      while (omit.contains(x)) {
        x++;
      }
      while (k < i) {
        k++; x++;
        while (omit.contains(x)) {
          x++;
        }
      }
      return x;
    }
  }


  /** Returns the entropy of this distribution, in nats.
    * @return -log(size()) */
  public double entropy() {
    return logp;
  }

  /** Returns the information entropy of a given element, in bits. */
  public double info(int k) {
    if (k >= lo && k <= hi) {
      return -logp / Tools.LN2;
    } else {
      return Double.POSITIVE_INFINITY;
    }
  }


  /** Returns a String description of the set containing
    * the range of integers from <var>a</var> to <var>b</var>. */
  protected static String intRangeString(int a, int b) {
    String s="{"+a;
    if (b-a > 3) {
      s+=",...,"+b+"}";
    } else {
      for (int k=a+1; k<=b; k++) {
        s+=","+k;
      }
      s+="}";
    }
    return s;
  }
  
  /** Expands the range of this uniform distribution upwards or
    * downwards by a given number of integers.
    * If the specified number is negative, the distribution is
    * expanded downwards from its lower boundary.  If the specified
    * number is positive, the distribution is expanded upwards from
    * its upper boundary.  The number of elements increases by abs(n)
    * in either case.
    * @param k number of integers by which to expand this uniform
    *          distribution, the sign of the number signifies the
    *          direction of the expansion. */
  public synchronized void expand(int k) {
    if (k > 0) {
      n+=k;
      hi+=k;
      logp = Double.NaN; // invalidate cache
    } else
    if (k < 0) {
      n+=k;
      lo-=k;
      logp = Double.NaN; // invalidate cache
    }
  }
  
  /** Shrinks the range of this uniform distribution.
    * If the specified number is negative, the distribution is shrunk
    * from its upper boundary downwards.  If the specified number is
    * positive, the distribution is shrunk upwards from its lower
    * boundary.
    * @param k number of integers by which to shrink this uniform
    *          distribution, the sign of the number signifies the
    *          direction. */
  public synchronized void shrink(int k) {
    if (k > 0) {
      n-=k;
      lo+=k;
      logp = Double.NaN; // invalidate cache
    } else
    if (k < 0) {
      n-=k;
      hi-=k;
      logp = Double.NaN; // invalidate cache
    }
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

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(lo);
    out.writeInt(hi);
  }

  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    lo = in.readInt();
    hi = in.readInt();
    n    = hi-lo+1;
    logp = -Math.log(n);
  }


}
