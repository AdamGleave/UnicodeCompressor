/* $Id: CRPU.java,v 1.13 2015/07/23 19:34:10 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Vector;
import java.util.Random;
import java.util.Collection;
import java.util.HashSet;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

/** Chinese Restaurant Process with base distribution, storing
  * <i>unique</i> values.
  * Samples produced by this process are distributed according
  * to a random draw from a Dirichlet Process or Pitman-Yor process.
  * The process takes a concentration parameter <var>α</var>,
  * discount parameter <var>β</var>, and base distribution
  * <code>base</code>. Integer arithmetic is used instead of
  * floating point arithmetic to facilitate encoding / decoding.
  * <dl><dt><b>Notes:</b></dt>
  * <dd><ul>
  * <li>This differs from the CRPV by storing the number of customers
  *     and tables for each unique dish (rather than storing customers
  *     and dish per table).</li>
  * <li>For base distributions with atoms (e.g. discrete distributions),
  *     this process exhibits 'generation ambiguity', i.e. samples from
  *     the process can be generated in two ways: either by recall or
  *     by resynthesis.</li>
  * </ul></dd></dl>
  * @see CRPI
  * @see CRPV
  * @see DP 
  * @see PDP */
public class CRPU<X> extends Distribution<X> implements DSP<X>,
                                                        AdaptiveCode<X>,
                                                        Externalizable {
  /** Concentration parameter: numerator. */
  int a1 = 1;
  /** Concentration parameter: denominator. */
  int a2 = 2;

  /** Discount parameter: numerator. */
  int b1 = 0;
  /** Discount parameter: denominator. */
  int b2 = 1;

  /** Base distribution. */
  Distribution<X> base;
 
  /** Number of unique dishes. */
  int u = 0;

  /** Total number of customers.
    * This is equal to the sum of entries in the <var>counts</var> vector. */
  int n = 0;

  /** Total number of tables.
    * This is equal to the sum of entries in the <var>tables</var> vector. */
  int t = 0;


  /** Unique dishes. */
  Vector<X> values = new Vector<X>();

  /** Number of customers for each unique dish. */
  Vector<Integer> counts = new Vector<Integer>();
  
  /** Number of tables for each unique dish. */
  Vector<Integer> tables = new Vector<Integer>();

  /** Precision for rational number conversion. */
  static final int prec = 0x20000;
  

  /** Empty constructor, for serialization. */
  public CRPU() {
  }


  /** Constructs a new CRP with concentration parameter
    * <var>a1/a2</var>, discount parameter <var>b1/b2</var>
    * and base distribution <var>base</var>. */
  public CRPU(int a1, int a2, int b1, int b2, Distribution<X> base) {
    this.a1 = a1;
    this.a2 = a2;
    this.b1 = b1;
    this.b2 = b2;
    this.base = base;
  }
  
  /** Constructs a new CRP with concentration parameter
    * <var>alpha</var>, discount parameter <var>beta</var>
    * and base distribution <var>base</var>.
    * <b>Note:</b> <var>alpha</var> and <var>beta</var> are converted to
    * fractions. */
  public CRPU(double alpha, double beta, Distribution<X> base) {
    Tuple<Long,Long> afrac = Tools.fraction(alpha, prec);
    Tuple<Long,Long> bfrac = Tools.fraction(beta, prec);
    this.a1 = afrac.get0().intValue();
    this.a2 = afrac.get1().intValue();
    this.b1 = bfrac.get0().intValue();
    this.b2 = bfrac.get1().intValue();
    this.base = base;
  }
  
  /** Constructs a new CRP identical to <var>r</var>. */
  public CRPU(CRPU<X> r) {
    this.a1 = r.a1;
    this.a2 = r.a2;
    this.b1 = r.b1;
    this.b2 = r.b2;
    this.n = r.n;
    this.t = r.t;
    this.u = r.u;
    this.base   = r.base.clone();
    this.counts = new Vector<Integer>(counts);
    this.tables = new Vector<Integer>(tables);
    this.values = new Vector<X>(values);
  }
  
  /** Returns a cloned copy of this CRP. */
  public CRPU<X> clone() {
    return new CRPU<X>(this);
  }
  
  
  /** Constructs a new CRPU with given parameters.
    * <br>
    * Example parameter string: "<tt>a=0:b=0.5</tt>".
    * Meaning of parameters:<br>
    * <dl><dt><b>Parameters and values:</b></dt><dd><ul>
    *   <li>field: <b>a</b>, type: double, strength parameter (alpha).</li>
    *   <li>field: <b>b</b>, type: double, discount parameter (beta).</li>
    * </ul></dd></dl>
    * @param pars parameters in String form
    * @param base base distribution over symbols */
  public static <X> CRPU<X> createNew(String pars, Distribution<X> base) {
    // defaults:
    double alpha = 0.0;
    double beta = 0.5;
    // now parse arguments
    String[] s = pars.split(":");
    for (int k=0; k<s.length; k++) {
      int eq = s[k].indexOf('=');
      if (eq != -1) {
        String key = s[k].substring(0,eq);
        String val = s[k].substring(eq+1);
        if (key.equals("a")) {
          // ALPHA parameter (concentration)
          alpha = Double.valueOf(val);
        } else
        if (key.equals("b")) {
          // BETA parameter (discount)
          beta = Double.valueOf(val);
        } else {
          System.err.println("Warning: CRPU: unknown parameter \""+s[k]+"\"");
        }
      }
    }
    return new CRPU<X>(alpha,beta,base);
  }


  /** Samples a dish from this CRP.
    * @return the dish */
  public X sample(Random rnd) {
    long z = b2*(n*a2 + a1);
    double r = (double) z * rnd.nextDouble();
    double mass = 0.0;
    for (int k=0; k < u; k++) {
      double p = (double) a2*(counts.get(k)*b2 - tables.get(k)*b1);
      mass+=p;
      if (mass > r) {
        // existing table
        counts.set(k,counts.get(k)+1);
        n++;
        return values.get(k);
      }
    }
    // otherwise: sample value from the base distribution
    X newdish = base.sample(rnd);
    int k=values.indexOf(newdish);
    if (k != -1) {
      counts.set(k,counts.get(k)+1);
      tables.set(k,tables.get(k)+1);
      t++; n++;
    } else {
      values.add(newdish); u++;
      counts.add(1); n++;
      tables.add(1); t++;
    }
    return newdish;
  }

  /** Returns the next dish sampled from the restaurant.
    * This is identical to <code>sample(rnd)</code>.
    * @see #sample(Random) */
  public X next(Random rnd) {
    return sample(rnd);
  }

  /** Adds a customer for a particular dish, seated
    * either at an existing table or a new table.
    * @param x dish whose customer count shall be incremented
    * @param newtable if true, a new table is created;
    *                 if false, an existing table is used. */
  public void add(X x, boolean newtable) {
    int k = values.indexOf(x);
    if (k == -1) {
      values.add(x); u++;
      counts.add(1); n++;
      tables.add(1); t++;
    } else {
      counts.set(k,counts.get(k)+1); n++;
      if (newtable) {
        tables.set(k,tables.get(k)+1); t++;
      }
    }
  }

  /** Improper learn method, always chooses existing table. */
  public void learn(X x) {
    add(x,false);
  }

  public long discreteTotalMass() {
    long bz = base.discreteTotalMass();
    return (long) b2*((long) n*a2 + a1) * bz;
  }

  public long discreteMass(X x) {
    int k = values.indexOf(x);
    long bz = base.discreteTotalMass();
    // recall portion
    long rec = 0;
    if (k != -1) {
      rec = (long) a2*bz*((long) counts.get(k)*b2 - tables.get(k)*b1);
    }
    // synthesis portion
    long syn = ((long) a1*b2 + (long) a2 * b1 * t)
                     * base.discreteMass(x);
    // return total mass
    return rec + syn;
  }
  
  public long discreteTotalMass(Iterable<X> col) {
    long mass = 0;
    long bz = base.discreteTotalMass();
    for (X x : col) {
      int k = values.indexOf(x);
      // recall portion
      long rec = (long) a2*bz*((long) counts.get(k)*b2 - tables.get(k)*b1);
      // synthesis portion
      long syn = ((long) a1*b2 + (long) a2 * b1 * t) * base.discreteMass(x);
      mass += rec + syn;
    }
    // return total mass
    return mass;
  }
  
  
  public void encode(X x, Encoder enc) {
    //encode(x, new HashSet<X>(), enc);
    long mass = 0;
    long bz = base.discreteTotalMass();
    long total = (long) b2*((long) n*a2 + a1) * bz;
    for (int k=0; k<u; k++) {
      X xk = values.get(k);
      long rec = (long) a2*bz*((long) counts.get(k)*b2
                             - (long) tables.get(k)*b1);
      long syn = ((long) a1*b2 + (long) a2*b1*t) * base.discreteMass(xk);
      if (xk.equals(x)) {
        enc.storeRegion(mass, mass+rec+syn, total);
        return;
      }
      mass += rec + syn;
    }
    // if we got here, we've hit a new element
    enc.storeRegion(mass, total, total);
    // encode using the base distribution, with omissions
    base.encode(x, values, enc);
  }


  public void encode(X x, Collection<X> omit, Encoder enc) {
    HashSet<X> baseomit = new HashSet<X>(omit); // setifies 'omit'
    long bz = base.discreteTotalMassWithout(baseomit);
    long tt = 0;   // total number of tables after omissions
    long nn = 0;   // total number of customers after omissions
    long recmass = 0;
    long synmass = 0;
    long lo_rec = -1; long lo_syn = -1;
    long hi_rec = -1; long hi_syn = -1;
    for (int k=0; k<u; k++) {
      X xk = values.get(k);
      if (!omit.contains(xk)) {
        long cm = counts.get(k);
        long tm = tables.get(k);
        long rec = (long) cm*b2 - (long) tm*b1;
        long syn = base.discreteMass(xk);
        /* In 'syn' above, factor (a1*b2 + a2*b1*tt) is missing, since
         * the number of tables after omissions, tt, is not yet known. */
        /* In 'rec' above, factor (a2*bz) is missing, for computational
         * efficiency. */
        nn += cm;
        tt += tm;
        baseomit.add(xk);    // xk is now accounted for: omit in base
        if (xk.equals(x)) {
          lo_rec = recmass;
          lo_syn = synmass;
          hi_rec = recmass+rec;
          hi_syn = synmass+syn;
        }
        recmass += rec;  // update (incomplete) cumulative recall mass
        synmass += syn;  // update (incomplete) cumulative synthesis mass
      } else {
        if (xk.equals(x)) {
          throw new ZeroMassException();
        }
      }
    }
    // now that total customers and tables are known, compute total
    long total = (long) b2*((long) nn*a2 + a1)*bz;
    // and compute the common factors
    long recmul = (long) a2*bz;
    long synmul = (long) a1*b2 + (long) a2*b1*tt;
    if (lo_rec != -1) {
      lo_rec *= recmul;
      hi_rec *= recmul;
      lo_syn *= synmul;
      hi_syn *= synmul;
      enc.storeRegion(lo_rec+lo_syn, hi_rec+hi_syn, total);
    } else {
      // if we got here, we've hit a new element
      recmass *= recmul;
      synmass *= synmul;
      enc.storeRegion(recmass + synmass, total, total);
      // encode using the base distribution, with revised omissions
      base.encode(x, baseomit, enc);
    }
  }
  
  
  public X decode(Decoder dec) {
    //return decode(new HashSet<X>(), dec);
    long bz = base.discreteTotalMass();
    long total = (long) b2*((long) n*a2 + a1) * bz;
    long aim = dec.getTarget(total);
    long lo;
    long hi = 0;
    for (int k=0; k<u; k++) {
      X x = values.get(k);
      long rec = (long) a2*bz*((long) counts.get(k)*b2
                             - (long) tables.get(k)*b1);
      long syn = ((long) a1*b2 + (long) a2*b1*t) * base.discreteMass(x);
      lo  = hi;
      hi += rec+syn;
      if (aim >= lo && aim < hi) {
        dec.loadRegion(lo, hi, total);
        return x;
      }
    }
    // if we got here, we've hit a new element
    dec.loadRegion(hi, total, total);
    // decode using the base distribution, with omissions
    return base.decode(values, dec);
  }
  
  
  public X decode(Collection<X> omit, Decoder dec) {
    long mass = 0;
    HashSet<X> baseomit = new HashSet<X>(omit); // setifies 'omit'
    long bz = base.discreteTotalMassWithout(baseomit);
    long tt = 0;   // total number of tables after omissions
    long nn = 0;   // total number of customers after omissions
    for (int k=0; k<u; k++) {
      X xk = values.get(k);
      if (!omit.contains(xk)) {
        tt += tables.get(k);
        nn += counts.get(k);
      }
    }
    // Now tt and nn are acquired.  Compute total, recmul and synmul.
    long total = (long) b2*((long) nn*a2 + a1)*bz;
    long recmul = (long) a2*bz;
    long synmul = (long) a1*b2 + (long) a2*b1*tt;
    
    long aim = dec.getTarget(total);

    long lo = 0;
    long hi = 0;
    for (int k=0; k<u; k++) {
      X xk = values.get(k);
      if (!omit.contains(xk)) {
        long cm = counts.get(k);
        long tm = tables.get(k);
        long rec = recmul * ((long) cm*b2 - (long) tm*b1);
        long syn = synmul * base.discreteMass(xk);
        lo = hi;
        hi = lo + rec + syn;
        if (aim >= lo && aim <= hi) {
          dec.loadRegion(lo,hi,total);
          return xk;
        }
        baseomit.add(xk);    // xk is now accounted for: omit in base
      }
    }
    // if we got here, we've hit a new element.
    dec.loadRegion(hi,total,total);
    return base.decode(baseomit, dec);
  }


  /** Probability mass of <var>x</var>.
    * @see #logMass(Object)
    * @see #density(Object) */
  public double mass(X x) {
    int k = values.indexOf(x);
    int nx = k != -1 ? counts.get(k) : 0;
    int tx = k != -1 ? tables.get(k) : 0;
    // normalisation constant
    long z = (long) b2*((long) n*a2 + a1);
    // recall portion
    long rec = (long) a2*((long) nx*b2
                               - tx*b1);
    // synthesis portion
    double syn = ((long) a1*b2 + (long) a2 * b1 * t)
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

  /** Probability density of <var>x</var>.
    * @see #logDensity(Object)
    * @see #mass(Object) */
  public double density(X x) {
    if (values.contains(x)) {
      return Double.POSITIVE_INFINITY;
    } else {
      double z = (long) b2*((long) n*a2 + a1);
      double syn = ((long) a1*b2 + (long) a2*b1*t) * base.density(x);
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
      double z = Math.log((long) b2*((long) n*a2 + a1));
      double syn = Math.log(  (long) a1*b2
                            + (long) a2*b1*t);
      return syn + base.logDensity(x) - z;
    }
  }

  public double p(X x) {
    throw new UnsupportedOperationException("choose mass or density");
  }
  public double logp(X x) {
    throw new UnsupportedOperationException("choose mass or density");
  }
  public Distribution<X> getPredictiveDistribution() {
    throw new UnsupportedOperationException();
  }


  public String toString() {
    if (n == 0) {
      return "CRPU(α="+(double)a1/a2
               +", β="+(double)b1/b2
               +", H="+base+")";
    } else {
      return "CRPU(α="+(double)a1/a2
               +", β="+(double)b1/b2
               +", H="+base+" | N="+n+", T="+t+", U="+u+", ...)";
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(a1);
    out.writeInt(a2);
    out.writeInt(b1);
    out.writeInt(b2);
    out.writeInt(n);
    out.writeInt(t);
    out.writeInt(u);
    out.writeObject(counts);
    out.writeObject(tables);
    out.writeObject(values);
    out.writeObject(base);
  }

  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    a1 = in.readInt();
    a2 = in.readInt();
    b1 = in.readInt();
    b2 = in.readInt();
    n = in.readInt();
    t = in.readInt();
    u = in.readInt();
    counts = (Vector<Integer>) in.readObject();
    tables = (Vector<Integer>) in.readObject();
    values = (Vector<X>) in.readObject();
    base = (Distribution<X>) in.readObject();
  }


  public static void main(String[] args) {
    // And here's a little demo.
    Random rnd = new Random();
    System.out.println("Chinese Restaurant Process");
    CRPU<Integer> crp = new CRPU<Integer>(1,2,1,2,Discrete.integers(60));
    for (int i=0; i<100; i++) {
      System.out.print(crp.sample(rnd)+" ");
    }
    System.out.println();
    System.out.println(crp.n+" customers, "+crp.t+" tables, "+crp.u+" unique dishes.");
    System.out.println("Values: "+crp.values);
    System.out.println("Counts: "+crp.counts);
    System.out.println("Tables: "+crp.tables);
    System.out.println("↑ "+crp);
    System.out.println("Histogram of posterior distribution:");
    Histogram h2 = Histogram.fromIntegers(crp, 0, 59, 9900);
    h2.print(System.out);
    System.out.println("↑ "+crp);
  }

}
