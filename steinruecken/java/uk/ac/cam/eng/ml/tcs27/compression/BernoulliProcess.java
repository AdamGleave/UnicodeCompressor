/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Random;
import java.util.Collection;
import java.io.Serializable;

/** Bernoulli process.
  * A binary-valued discrete stochastic process whose sample
  * path are outcomes from independent Bernoulli trials with
  * a shared latent Beta-distributed bias <var>θ</var>.
  * The process is parametrised by two positive real numbers
  * <var>α</var> and <var>β</var>, which have the same meaning
  * as for the Beta distribution.
  * @see Beta
  * @see Bernoulli
  * @see BetaBinomial */
public class BernoulliProcess<X> extends SimpleMass<X>
                                 implements AdaptiveCode<X> {
 
  /** Numerator of <var>α</var>. */
  short a0 = 1;
  /** Denominator of <var>α</var>. */
  short a1 = 2;
  /** Numerator of <var>β</var>. */
  short b0 = 1;
  /** Denominator of <var>β</var>. */
  short b1 = 2;
  /** Number of positive outcomes. */
  int na = 0;
  /** Number of negative outcomes. */
  int nb = 0;
  /** Value of a positive outcome. */
  X xa = null;
  /** Value of a negative outcome. */
  X xb = null;
  
  /** Constructs a new BernoulliProcess.
    * @param xa value for trial success
    * @param xb value for trial failure
    * @param a0 numerator of alpha
    * @param a1 denominator of alpha
    * @param b0 numerator of beta
    * @param b1 denominator of beta */
  public BernoulliProcess(X xa, X xb, short a0, short a1, short b0, short b1) {
    this.xa = xa;
    this.xb = xb;
    this.a0 = a0;
    this.a1 = a1;
    this.b0 = b0;
    this.b1 = b1;
    this.na = 0;
    this.nb = 0;
  }
  
  /** Constructs a new BernoulliProcess with α=1/2, β=1/2.
    * @param xa value for trial success
    * @param xb value for trial failure */
  public BernoulliProcess(X xa, X xb) {
    this(xa,xb, (short)1, (short)2, (short)1, (short)2);
  }

  /** Returns a copy of this BernoulliProcess distribution. */
  public BernoulliProcess<X> clone() {
    BernoulliProcess<X> bp = new BernoulliProcess<X>(xa,xb,a0,a1,b0,b1);
    bp.na = this.na;
    bp.nb = this.nb;
    return bp;
  }

  long nextLong(Random rnd, long n) {
    long bits, val;
    do {
      bits = (rnd.nextLong() << 1) >>> 1;
      val = bits % n;
    } while (bits-val+(n-1) < 0L);
    return val;
  }

  /** Samples from this process.
    * This advances the internal process state. */
  public X sample(Random rnd) {
    int a1b1 = (int)a1*b1;
    long num = ((long)na+nb)*a1b1 + (int)a0*b1 + (int)a1*b0;
    long det = (long)na*a1b1 + (int)a0*b1;
    long r = nextLong(rnd, num);
    if (r < det) {
      na++;
      return xa;
    } else {
      nb++;
      return xb;
    }
  }

  public void encode(X x, Encoder ec) {
    int a1b1 = (int)a1*b1;
    int a1b0 = (int)a1*b0;
    int a0b1 = (int)a0*b1;
    long num = ((long)na+nb)*a1b1 + a0b1 + a1b0;
    long det = (long)a1b1*na + a0b1;
    if (xa.equals(x)) {
      ec.storeRegion(0, det, num);
    } else
    if (xb.equals(x)) {
      ec.storeRegion(det, num, num);
    } else {
      throw new IllegalArgumentException("attempt to encode impossible event");
    }
  }
  
  public X decode(Decoder dc) {
    int a1b1 = (int)a1*b1;
    int a1b0 = (int)a1*b0;
    int a0b1 = (int)a0*b1;
    long num = ((long)na+nb)*a1b1 + a0b1 + a1b0;
    long det = (long)a1b1*na + a0b1;
    long t = dc.getTarget(num);
    if (t < det) {
      dc.loadRegion(0, det, num);
      return xa;
    } else {
      dc.loadRegion(det, num, num);
      return xb;
    }
  }
  
  /** Encodes symbol <var>x</var> assuming that some probability mass
    * has been removed.
    * Negative observations (ra,rb) are subtracted from the number
    * of positive observations (na,nb).
    * The contribution of prior parameters α and β are reduced
    * in proportion to scaling factors (sa0/sa1) and (sb0/sb1).
    * @param x value to be encoded
    * @param ra removals for value xa 
    * @param rb removals for value xb
    * @param sa0 scaling factor for alpha (numerator)
    * @param sa1 scaling factor for alpha (denominator)
    * @param sb0 scaling factor for beta (numerator)
    * @param sb1 scaling factor for beta (denominator)
    * @param ec the encoder */
  public void encode(X x, int ra, int rb,
                          int sa0, int sa1, int sb0, int sb1, Encoder ec) {
    long a1b1 = (long)a1*sa1*b1*sb1;
    long a1b0 = (long)a1*sa1*b0*sb0;
    long a0b1 = (long)a0*sa0*b1*sb1;
    long num = ((long)na+nb-ra-rb)*a1b1 + a0b1 + a1b0;
    long det = a1b1*(na-ra) + a0b1;
    if (det == num || det == 0) {
      // certain event
    } else
    if (xa.equals(x)) {
      ec.storeRegion(0, det, num);
    } else
    if (xb.equals(x)) {
      ec.storeRegion(det, num, num);
    } else {
      throw new IllegalArgumentException("attempt to encode impossible event");
    }
  }
  
  /** Decodes a symbol, assuming that some probability mass
    * has been removed.
    * Negative observations (ra,rb) are subtracted from the number
    * of positive observations (na,nb).
    * The contribution of prior parameters α and β are reduced
    * in proportion to scaling factors (sa0/sa1) and (sb0/sb1).
    * @param ra removals for value xa 
    * @param rb removals for value xb
    * @param sa0 scaling factor for alpha (numerator)
    * @param sa1 scaling factor for alpha (denominator)
    * @param sb0 scaling factor for beta (numerator)
    * @param sb1 scaling factor for beta (denominator)
    * @param dc the decoder */
  public X decode(int ra, int rb, int sa0, int sa1, int sb0, int sb1,
                                                             Decoder dc) {
    long a1b1 = (long)a1*sa1*b1*sb1;
    long a1b0 = (long)a1*sa1*b0*sb0;
    long a0b1 = (long)a0*sa0*b1*sb1;
    long num = ((long)na+nb-ra-rb)*a1b1 + a0b1 + a1b0;
    long det = a1b1*(na-ra) + a0b1;
    if (det == num) {
      // certain event
      return xa;
    } else
    if (det == 0) {
      // certain event
      return xb;
    } else {
      long t = dc.getTarget(num);
      if (t < det) {
        dc.loadRegion(0, det, num);
        return xa;
      } else {
        dc.loadRegion(det, num, num);
        return xb;
      }
    }
  }
  
  public void encode(X x, Collection<X> omit, Encoder ec) {
    boolean oa = omit.contains(xa);
    boolean ob = omit.contains(xb);
    if ((oa && ob) || (!xa.equals(x) && (!xb.equals(x)))) {
      throw new RuntimeException("attempt to encode impossible event");
    } else
    if (!oa && !ob) {
      // no omission: same as usual
      encode(x,ec);
    }
    /* The remaining events are certain, and therefore need no encoding. */
  }
  
  public X decode(Collection<X> omit, Decoder dc) {
    boolean oa = omit.contains(xa);
    boolean ob = omit.contains(xb);
    if (oa) {
      if (ob) {
        throw new RuntimeException("no probability mass left");
      } else {
        return xb;  // with certainty
      }
    } else {
      if (ob) {
        return xa;  // with certainty
      } else {
        return decode(dc);
      }
    }
  }

  /** Returns the log probability mass of outcome <var>x</var>. */
  public double logMass(X x) {
    int a1b1 = (int)a1*b1;
    int a1b0 = (int)a1*b0;
    int a0b1 = (int)a0*b1;
    double lz = Math.log(((long)na+nb)*a1b1 + a0b1 + a1b0);
    if (xa.equals(x)) {
      return (double) Math.log((long)a1b1*na + a0b1) - lz;
    } else
    if (xb.equals(x)) {
      return (double) Math.log((long)a1b1*nb + a1b0) - lz;
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }

  /** Returns the probability mass of outcome <var>x</var>. */
  public double mass(X x) {
    int a1b1 = (int)a1*b1;
    int a1b0 = (int)a1*b0;
    int a0b1 = (int)a0*b1;
    double num = ((long)na+nb)*a1b1 + a0b1 + a1b0;
    if (xa.equals(x)) {
      return ((long)a1b1*na + a0b1)/num;
    } else
    if (xb.equals(x)) {
      return ((long)a1b1*nb + a1b0)/num;
    } else {
      return 0;
    }
  }

  /** Returns the total probability mass of elements in <var>col</var>. */
  public double totalMass(Iterable<X> col) {
    int a1b1 = (int)a1*b1;
    int a1b0 = (int)a1*b0;
    int a0b1 = (int)a0*b1;
    double num = ((long)na+nb)*a1b1 + a0b1 + a1b0;
    long mass = 0;
    for (X x : col) {
      if (xa.equals(x)) {
        mass += (long)a1b1*na + a0b1;
      } else
      if (xb.equals(x)) {
        mass += (long)a1b1*nb + a1b0;
      }
    }
    return mass / num;
  }

  /** Advances the process by observing value <var>x</var>.
    * @throws IllegalArgumentException if <var>x</var> is not a legal value. */
  public void learn(X x) {
    if (xa.equals(x)) {
      na++;
    } else
    if (xb.equals(x)) {
      nb++;
    } else {
      throw new IllegalArgumentException();
    }
  }
  
  public Bernoulli<X> getPredictiveDistribution() {
    int a1b1 = (int)a1*b1;
    int a1b0 = (int)a1*b0;
    int a0b1 = (int)a0*b1;
    double num = ((long)na+nb)*a1b1 + a0b1 + a1b0;
    double theta = ((long)a1b1*na + a0b1)/num;
    return new Bernoulli<X>(xa, xb, theta);
  }

  public int getCount(X x) {
    if (x == xa) {
      return na;
    } else if (x == xb) {
      return nb;
    } else {
      return 0;
    }
  }

  public String toString() {
    return "BernoulliProcess(na = " + na + ", a0/a1 = " + a0 + "/" + a1 +
                             ", nb = " + nb + ", b0/b1 = " + b0 + "/" + b1 + ")";
  }
}