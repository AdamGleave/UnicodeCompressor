/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;

/** Implements the Bernoulli distribution.
  * <div>This is a discrete distribution over two possible outcomes
  * <var>x1</var> and <var>x0</var>, parametrised by the
  * probability <var>p</var> of obtaining outcome <var>x1</var>.</div>
  * <div>
  * <b>Notes</b>:
  * <ul><li>Conjugate prior for <var>p</var> is the
  *     <i>Beta distribution</i>.</li>
  *     <li>The sum of <var>n</var> draws from an integer-valued
  *     Bernoulli distribution is modelled by a <i>Binomial distribution</i>.
  *     </li>
  *     <li>The number of failures before a first success in a sequence
        of independent Bernoulli draws is modelled by
  *     the <i>Geometric distribution</i>.
  *     </li>
  *     <li>The Bernoulli distribution with its two outcomes is a special
  *     case of the <i>Discrete distribution</i>, which is defined for
  *     any (fixed) number of outcomes.</li>
  * </ul>
  * The encoding and decoding methods handle extreme probability
  * events by 'rounding up' the probability to the arithmetic coder's
  * minimum bin size.
  * The result depends on the resolution of the arithmetic coder.
  * </div>
  * @see Binomial
  * @see Geometric
  * @see Discrete
  * @see Beta */
public class Bernoulli<X> extends SimpleMass<X>
                          implements Codable<X>, Iterable<X> {
  
  /** Bias of this Bernoulli distribution. */
  protected double p = 0.5;

  /** Element with probability (1-p). */
  X x0 = null;
  /** Element with probability p. */
  X x1 = null;

  /** Creates a fair Bernoulli distribution over
    * values <var>x1</var> and <var>x0</var>. */
  public Bernoulli(X x1, X x0) {
    this.x0 = x0;
    this.x1 = x1;
  }
  
  /** Creates Bernoulli distribution with values <var>x1</var> and
    * <var>x0</var> and bias <var>p</var>.
    * Value <var>x1</var> has probability <var>p</var>,
    * and value <var>x0</var> probability (1-<var>p</var>). */
  public Bernoulli(X x1, X x0, double p) {
    assert (p >= 0.0 && p <= 1.0);
    this.x0 = x0;
    this.x1 = x1;
    this.p = p;
  }
  
  /** Creates Bernoulli distribution with values <var>x1</var> and
    * <var>x0</var> and bias <var>a/b</var>.
    * Value <var>x1</var> has probability <var>a/b</var>,
    * and value <var>x0</var> probability <var>(b-a)/b</var>). */
  public Bernoulli(X x1, X x0, int a, int b) {
    this.x0 = x0;
    this.x1 = x1;
    this.p = (double) a / (double) b;
  }

  /** Creates a Bernoulli distribution over booleans
    * <i>true</i> and <i>false</i>, with bias <var>p</var>.
    * Note: <i>true</i> has probability <var>p</var>,
    * <i>false</i> has probability (1-<var>p</var>). */
  public static Bernoulli<Boolean> booleans(double p) {
    return new Bernoulli<Boolean>(true, false, p);
  }
  
  /** Creates a Bernoulli distribution over bytes 0 and 1
    * with bias <var>p</var>.
    * Value 1 has probability <var>p</var>, value 0 has
    * probability (1-<var>p</var>). */
  public static Bernoulli<Byte> bytes(double p) {
    return new Bernoulli<Byte>((byte) 1, (byte) 0, p);
  }

  /** Creates a Bernoulli distribution over integers 0 and 1
    * with bias <var>p</var>.
    * Value 1 has probability <var>p</var>, value 0 has
    * probability (1-<var>p</var>). */
  public static Bernoulli<Integer> integers(double p) {
    return new Bernoulli<Integer>(1, 0, p);
  }
  
  public String toString() {
    return "Bernoulli("+x1+":"+p+", "+x0+":"+(1.0-p)+")";
  }

  /** Returns the probability mass for outcome <var>x</var>. */
  public double mass(X x) {
    if (x.equals(x1)) {
      return p;
    } else
    if (x.equals(x0)) {
      return 1.0-p;
    } else {
      return 0.0;
      // or should we throw IllegalArgumentException...?
    }
  }
  
  /** Returns the log probability mass for outcome <var>x</var>. */
  public double logMass(X x) {
    if (x.equals(x1)) {
      return Math.log(p);
    } else
    if (x.equals(x0)) {
      return Math.log(1.0-p);
    } else {
      return Double.NEGATIVE_INFINITY;
      // or should we throw IllegalArgumentException...?
    }
  }

  public double totalMass(Iterable<X> col) {
    double mass = 0.0;
    for (X x : col) {
      if (x0.equals(x)) {
        mass += 1.0-p;
      } else
      if (x1.equals(x)) {
        mass += p;
      }
    }
    return mass;
  }

  /** Samples from this Bernoulli distribution. */
  public X sample(Random rnd) {
    return (rnd.nextDouble() < p ? x1 : x0);
  }
  
  /** Samples from this Bernoulli distribution, omitting elements specified
    * in <var>omit</var>. */
  public X sampleWithout(Collection<X> omit, Random rnd) {
    if (omit.contains(x1)) {
      if (omit.contains(x0)) {
        throw new IllegalArgumentException("No probability mass left.");
      } else {
        // with certainty
        return x0;
      }
    } else
    if (omit.contains(x0)) {
      // with certainty
      return x1;
    } else {
      // TODO: switch to integer arithmetic?
      return (rnd.nextDouble() < p ? x1 : x0);
    }
  }
 
  /** Returns the mode of this Bernoulli distribution. */
  public X mode() {
    // at p=0.5 the mode is both at 0 and at 1.
    return (p > 0.5) ? x1 : x0;
    // TODO: switch to integer arithmetic?
  }

  /** Returns the entropy of this Bernoulli distribution.
    * @return the entropy, in nats. */
  public double entropy() {
    return - p*Math.log(p) - (1.0-p)*Math.log(1.0-p);
  }
  
  /** Returns the amount of information conveyed by a particular
    * outcome (in bits).
    * @return the information, in bits. */
  public double info(X x) {
    return (x == x1) ? - Math.log(p)/Tools.LN2
                     : - Math.log(1.0-p)/Tools.LN2;
  }

  /** Clones this Bernoulli distribution. */
  public Bernoulli<X> clone() {
    // FIXME: save discretization information (q,z) also!
    return new Bernoulli<X>(x0,x1,p);
  }

  /** Returns if this distribution is defined on a finite set of
    * elements. The Bernoulli distribution is defined on only two
    * elements, and two was a finite number last time I checked.
    * @return true */
  public boolean isFinite() {
    return true;
  }

  /** Returns an Iterator over the two elements. */
  public Iterator<X> iterator() {
    return new Iterator<X>() {
      X last = null;
      public X next() {
        if (last == null) {
          last = x0;
        } else
        if (last == x0) {
          last = x1;
        } else
        if (last == x1) { 
          throw new java.util.NoSuchElementException();
        }
        return last;
      }
      public boolean hasNext() {
        return (last == null || last == x0);
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }


  public void encode(X x, Encoder ec) {
    if (p == 1.0) {
      if (!x1.equals(x)) { throw new ZeroMassException(); }
    } else
    if (p == 0.0) {
      if (!x0.equals(x)) { throw new ZeroMassException(); }
    } else {
      long range = ec.getRange();
      long cut = (long) ((double) range * p);
      // ensure the coded probability mass cannot be zero
      if (cut == 0L) {
        cut = 1L;
      } else
      if (cut == range) {
        cut = range - 1;
      }
      // encode it
      if (x1.equals(x)) {
        ec.storeRegion(0,cut);
      } else
      if (x0.equals(x)) {
        ec.storeRegion(cut,range);
      } else {
        throw new IllegalArgumentException("attempt to encode an undefined value");
      }
    }
  }

  public void encode(X x, Collection<X> without, Encoder ec) {
    if (without.contains(x)) {
      throw new IllegalArgumentException("attempt to encode an excluded element");
    } else
    if (x1.equals(x)) {
      if (without.contains(x0)) {
        // certain event: no need to store anything
      } else {
        // encode normally
        encode(x,ec);
      }
    } else
    if (x0.equals(x)) {
      if (without.contains(x1)) {
        // certain event: no need to store anything
      } else {
        // encode normally
        encode(x,ec);
      }
    } else {
      throw new IllegalArgumentException("attempt to encode an undefined value");
    }
  }
  
  public X decode(Decoder dc) {
    if (p == 1.0) {
      return x1;
    } else
    if (p == 0.0) {
      return x0;
    } else {
      long range = dc.getRange();
      long cut = (long) ((double) range * p);
      // ensure the coded probability mass cannot be zero
      if (cut == 0L) {
        cut = 1L;
      } else
      if (cut == range) {
        cut = range - 1;
      }
      // decode
      long i = dc.getTarget(range);
      if (i < cut) {
        dc.loadRegion(0,cut);
        return x1;
      } else {
        dc.loadRegion(cut,range);
        return x0;
      }
    }
  }
  
  public X decode(Collection<X> without, Decoder dc) {
    if (without.contains(x0)) {
      if (without.contains(x1)) {
        throw new ZeroMassException("attempt to decode an excluded event");
      } else {
        // certain outcome: no rescaling
        return x1;
      }
    } else {
      if (without.contains(x1)) {
        // certain outcome: no rescaling
        return x0;
      } else {
        // otherwise fall back to default case
        return decode(dc);
      }
    }
  }

}
