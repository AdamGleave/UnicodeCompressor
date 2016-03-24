/* Automated copy from build process */
/* $Id: CRPV.java,v 1.5 2012/04/12 09:04:48 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Vector;
import java.util.Random;
import java.util.Collection;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

/** Chinese Restaurant Process with base distribution.
  * Samples produced by this process are distributed according
  * to a random draw from a Dirichlet Process or Pitman-Yor process.
  * The process takes a concentration parameter <var>α</var>,
  * discount parameter <var>β</var>, and base distribution
  * <code>base</code>. Integer arithmetic is used instead of
  * floating point arithmetic to facilitate encoding / decoding.
  * <dl><dt><b>Notes:</b></dt>
  * <dd><ul>
  * <li>This extends the basic CRPI by adding a kitchen
  *     to the restaurant, and dishes to each table.</li>
  * <li>One value is stored for each table, even if values repeat.
  *     An alternative implementation is CRPU, which instead stores
  *     unique values and how many tables there are per value.</li>
  * <li>For base distributions with atoms (e.g. discrete distributions),
  *     this process exhibits 'generation ambiguity', i.e. samples from
  *     the process can be generated in two ways: either by recall or
  *     by resynthesis.</li>
  * </ul></dd></dl>
  * @see CRPI
  * @see CRPU
  * @see DP 
  * @see PDP */
public class CRPV<X> extends Distribution<X> implements DSP<X>,
                                                        Externalizable {
 
  /** Underlying combinatorial Chinese Restaurant Process. */
  CRPI crp;

  /** Base distribution. */
  Distribution<X> base;
  
  /** Dishes.  These are values sampled from the <var>base</var>. */
  Vector<X> values = new Vector<X>();
  
  /** Empty constructor, for serialization. */
  public CRPV() {
  }


  /** Constructs a new CRP with concentration parameter
    * <var>crp.a1/crp.a2</var>, discount parameter <var>crp.b1/crp.b2</var>
    * and base distribution <var>base</var>. */
  public CRPV(int a1, int a2, int b1, int b2, Distribution<X> base) {
    this.crp = new CRPI(a1, a2, b1, b2);
    this.base = base;
  }
  
  /** Constructs a new CRP with concentration parameter
    * <var>alpha</var>, discount parameter <var>beta</var>
    * and base distribution <var>base</var>.
    * <b>Note:</b> <var>alpha</var> and <var>beta</var> are converted to
    * fractions. */
  public CRPV(double alpha, double beta, Distribution<X> base) {
    this.crp = new CRPI(alpha, beta);
    this.base = base;
  }
  
  /** Constructs a new CRP identical to <var>r</var>. */
  public CRPV(CRPV<X> r) {
    this.crp    = r.crp.clone();
    this.base   = r.base.clone();
    this.values = new Vector<X>(values);
  }
  
  /** Returns a cloned copy of this CRP. */
  public CRPV<X> clone() {
    return new CRPV<X>(this);
  }

  /** Return a dish sampled from the restaurant.
    * This advances the CRP and hence changes the distribution.
    * This is identical to <code>sample(rnd)</code>.
    * @see #sample(Random) */
  public X next(Random rnd) {
    return sample(rnd);
  }

  /** Probability mass of <var>x</var>.
    * @see #logMass(Object)
    * @see #density(Object) */
  public double mass(X x) {
    int d = 0; // number of tables x is served at
    int c = 0; // number of customers eating x (at all tables)
    for (int k=0; k<crp.t; k++) {
      if (values.get(k).equals(x)) {
        d++;
        c+=crp.counts.get(k);
      }
    }
    // normalisation constant
    long z = (long) crp.b2*((long) crp.n*crp.a2 + crp.a1);
    // recall portion
    long rec = (long) crp.a2*((long) c*crp.b2 - d*crp.b1);
    // synthesis portion
    double syn = ((long) crp.a1*crp.b2 + (long) crp.a2*crp.b1*crp.t)
                 * base.mass(x);
    // return total mass
    return (rec+syn) / (double) z;
  }
  
  /** Log probability mass of <var>x</var>.
    * This implementation simply relays <code>mass(x)</code>.
    * @return Math.log(mass(x))
    * @see #mass(Object)
    * @see #logDensity(Object) */
  public double logMass(X x) {
    return Math.log(mass(x));
  }
 
  /** Total probability mass of all elements in <var>col</var>.
    * @return the sum of all <code>mass(x)</code> for <var>x</var>
    * in <var>col</var>.
    * @see #mass(Object) */
  public double totalMass(Iterable<X> col) {
    // TODO: improve this.
    double mass = 0.0;
    for (X x : col) {
      mass += mass(x);
    }
    return mass;
  }
  
  /** Probability density of <var>x</var>.
    * @see #logDensity(Object)
    * @see #mass(Object) */
  public double density(X x) {
    if (values.contains(x)) {
      return Double.POSITIVE_INFINITY;
    } else {
      double z = (long) crp.b2*((long) crp.n*crp.a2 + crp.a1);
      double syn = ((long) crp.a1*crp.b2 + (long) crp.a2*crp.b1*crp.t) * base.density(x);
      return syn / z;
    }
  }
  
  /** Log probability density of <var>x</var>.
    * @see #density(Object)
    * @see #logMass(Object) */
  public double logDensity(X x) {
    if (values.contains(x)) {
      return Double.POSITIVE_INFINITY;
    } else {
      double z = Math.log((long) crp.b2*((long) crp.n*crp.a2 + crp.a1));
      double syn = Math.log(  (long) crp.a1*crp.b2
                            + (long) crp.a2*crp.b1*crp.t);
      return syn + base.logDensity(x) - z;
    }
  }


  public double p(X x) {
    throw new UnsupportedOperationException("choose mass or density");
  }
  public double logp(X x) {
    throw new UnsupportedOperationException("choose mass or density");
  }
  

  /** Samples a dish from this CRP.
    * @return the dish */
  public X sample(Random rnd) {
    int t = crp.t;
    int k = crp.sample(rnd);
    if (k == t) {
      // new table
      X newdish = base.sample(rnd);
      values.add(newdish);
      return newdish;
    } else {
      // existing table
      return values.get(k);
    }
  }

  /** Adds a customer for given dish and table.
    * The customer count is incremented at the <var>j</var>th table
    * serving dish <var>x</var>. */
  public void add(X x, int j) {
    int pos = 0;
    for (int k=0; k < crp.t; k++) {
      if (values.get(k).equals(x)) {
        if (pos == j) {
          crp.counts.set(k,crp.counts.get(k)+1);
          crp.n++;
          return;
        } else {
          pos++;
        }
      }
    }
    // table not found
    if (pos == j) {
      values.add(x);
      crp.counts.add(1);
      crp.n++; crp.t++;
    } else {
      throw new IllegalArgumentException("no such table");
    }
  }

  public String toString() {
    if (crp.n == 0) {
      return "CRPV(α="+(double)crp.a1/crp.a2
               +", β="+(double)crp.b1/crp.b2
               +", H="+base+")";
    } else {
      return "CRPV(α="+(double)crp.a1/crp.a2
               +", β="+(double)crp.b1/crp.b2
               +", H="+base+" | N="+crp.n+", T="+crp.t+", ...)";
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    // alpha base values counts tables m n t
    out.writeObject(crp);
    out.writeObject(values);
    out.writeObject(base);
  }

  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    crp = (CRPI) in.readObject();
    values = (Vector<X>) in.readObject();
    base = (Distribution<X>) in.readObject();
  }


  public static void main(String[] args) {
    // And here's a little demo.
    Random rnd = new Random();
    System.out.println("Chinese Restaurant Process");
    CRPV<Integer> crp = new CRPV<Integer>(1,2,1,2,Discrete.integers(60));
    for (int i=0; i<100; i++) {
      System.out.print(crp.sample(rnd)+" ");
    }
    System.out.println();
    System.out.println(crp.crp.n+" customers, "+crp.crp.t+" tables.");
    System.out.println("Values: "+crp.values);
    System.out.println("Counts: "+crp.crp.counts);
    System.out.println("Histogram of posterior distribution:");
    Histogram h2 = Histogram.fromIntegers(crp, 0, 59, 9900);
    h2.print(System.out);
    System.out.println("↑ "+crp);
  }

}
