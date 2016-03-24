/* Automated copy from build process */
/* $Id: CRPI.java,v 1.10 2012/10/12 08:59:28 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Vector;
import java.util.Random;
import java.util.Collection;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

/** Chinese Restaurant Process (combinatorial object over integers).
  * Integers drawn from this process can be used to index a sequence
  * of iid samples from an arbitrary base distribution; the resulting
  * sequence then forms a random draw from a Dirichlet Process (β=0)
  * or Pitman-Yor Process (β &gt; 0).
  * <p>The successive integers returned by this implementation are the
  * table numbers at which customers in the restaurant are seated.
  * The restaurant seats customers at existing tables in proportion
  * to the number of customers already seated there (minus discount
  * parameter β), and creates new tables with probability proportional
  * to (α-Tβ), where T is the current number of tables.  The
  * restaurant starts out with zero tables, adding its first table (0)
  * with on the first draw.</p>
  * <dl><dt><b>Notes:</b></dt>
  * <dd>
  *     The CRPI only generates a sequence of table numbers.
  *     The CRPV augments this process by adding samples from a
  *     base distribution (dishes) to each table.
  * </dd></dl>
  * @see CRPV
  * @see CRPU
  * @see DP
  * @see PDP
  * @see GEM */
public class CRPI extends SimpleMass<Integer> implements DSP<Integer>,
                                                         AdaptiveCode<Integer>,
                                                         Externalizable {
 
  /** Concentration parameter: numerator. */
  int a1 = 1;
  /** Concentration parameter: denominator. */
  int a2 = 2;
  
  /** Discount parameter: numerator. */
  int b1 = 0;
  /** Discount parameter: denominator. */
  int b2 = 1;


  /** Total number of customers.
    * This is equal to the sum of <var>counts</var>. */
  int n = 0;

  /** Total number of tables. */
  int t = 0;

  /** Number of customers for each table. */
  Vector<Integer> counts = new Vector<Integer>();

  /** Precision for rational number conversion. */
  static final int prec = 0x20000;

  
  /** Empty constructor, for serialization. */
  public CRPI() {
  }

  /** Constructs a new CRP with concentration parameter <var>a1/a2</var>
    * and discount parameter <var>b1/b2</var>. */
  public CRPI(int a1, int a2, int b1, int b2) {
    this.a1 = a1;
    this.a2 = a2;
    this.b1 = b1;
    this.b2 = b2;
  }
  
  /** Constructs a new CRP with concentration parameter <var>alpha</var>
    * and discount parameter <var>beta</var>.<br>
    * <b>Note:</b> <var>alpha</var> and <var>beta</var> are converted
    * to fractions. */
  public CRPI(double alpha, double beta) {
    Tuple<Long,Long> afrac = Tools.fraction(alpha, prec);
    Tuple<Long,Long> bfrac = Tools.fraction(beta, prec);
    this.a1 = afrac.get0().intValue();
    this.a2 = afrac.get1().intValue();
    this.b1 = bfrac.get0().intValue();
    this.b2 = bfrac.get1().intValue();
  }

  
  /** Constructs a CRP with parameters <var>a1/a2</var> and
    * <var>b1/b2</var> and seating plan.
    * The seating plan is simply a list of customer counts per table.
    * @param counts number of customers per table */
  public CRPI(int a1, int a2, int b1, int b2, Iterable<Integer> counts) {
    this(a1,a2,b1,b2);
    for (int k : counts) {
      n+=k; // total number of customers
      t++;  // total number of tables
      this.counts.add(k);
    }
  }

  /** Constructs a CRP identical to the one supplied in the argument. */
  public CRPI(CRPI crp) {
    this.a1 = crp.a1; this.a2 = crp.a2;
    this.b1 = crp.b1; this.b2 = crp.b2;
    this.n = crp.n;
    this.t = crp.t;
    this.counts = new Vector<Integer>(crp.counts);
  }

  /** Returns a cloned copy of this CRP. */
  public CRPI clone() {
    return new CRPI(this);
  }


  /** Samples the next table number from this CRP.
    * Returns a number from 0 to <var>t</var>,
    * where 0..<var>t</var>-1 indicate an existing table was chosen,
    * and <var>t</var> indicates a new table was added. */
  public Integer sample(Random rnd) {
    long z = b2*(n*a2 + a1);
    double u = (double) z * rnd.nextDouble();
    double mass = 0.0;
    for (int k=0; k<t; k++) {
      double p = (double) a2*(counts.get(k)*b2 - b1);
      mass+=p;
      if (mass > u) {
        // existing table
        counts.set(k,counts.get(k)+1);
        n++;
        return k;
      }
    }
    // otherwise: new table
    counts.add(1);
    n++; t++;
    return (t-1);
    // NOTE: now mass should approximately equal:
    //    1.0 - (alpha+t*beta) / (n+alpha).
  }

  /** Returns the next table assignment.
    * This simple calls <code>sample(rnd)</code>.
    * @see #sample(Random) */
  public Integer next(Random rnd) {
    return sample(rnd);
  }

  /** Learns the next table allocation.
    * This updates the state of the restaurant.
    * @param k table number (0..t), where t means "new table". */
  public void learn(Integer k) {
    if (k < 0 || k > t) {
      throw new IllegalArgumentException("invalid table: "+k);
    } else
    if (k == t) {
      counts.add(1);
      t++; n++;
    } else {
      counts.set(k, counts.get(k)+1);
      n++;
    }
  }

  /** Probability mass assigned to table <var>k</var>.
    * @param k valid table number (0..t), where t means "new table"
    * @see #logMass(Integer) */
  public double mass(Integer k) {
    if (k < 0) {
      return 0.0;
    } else
    if (k < t) {
      // existing table
      return (double) a2*(counts.get(k)*b2 - b1) / (b2*(n*a2 + a1));
    } else
    if (k == t) {
      // new table
      return (double) (a1*b2 + a2*b1*t) / (b2*(n*a2 + a1));
    } else {
      return 0.0;
    }
  }
  
  /** Log probability mass assigned to table <var>k</var>.
    * @see #mass(Integer) */
  public double logMass(Integer k) {
    if (k < 0) {
      return Double.NEGATIVE_INFINITY;
    } else
    if (k < t) {
      // existing table
      return Math.log(a2*(counts.get(k)*b2 - b1)) - Math.log(b2*(n*a2 + a1));
    } else
    if (k == t) {
      // new table
      return Math.log(a1*b2 + a2*b1*t) - Math.log(b2*(n*a2 + a1));
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }

  
  /** Total probability mass of all elements in <var>col</var>.
    * @return the sum of all <code>mass(x)</code> for <var>x</var>
    * in <var>col</var>.
    * @see #mass(Object) */
  public double totalMass(Iterable<Integer> col) {
    double mass = 0.0;
    for (int k : col) {
      if (k >= 0 && k < t) {
        // existing table
        mass += (double) a2*(counts.get(k)*b2 - b1);
      } else
      if (k == t) {
        // new table
        mass += (double) (a1*b2 + a2*b1*t);
      }
    }
    return mass / (b2*(n*a2 + a1));
  }

  /** Merges two tables.
    * Moves customers from table <var>k2</var> to table <var>k1</var>,
    * and then deletes <var>k2</var>.
    * @param k1 table to be kept
    * @param k2 table to be removed */
  public void merge(Integer k1, Integer k2) {
    if (k1 < 0 || k1 > t || k2 < 0 || k2 > t) {
      throw new IllegalArgumentException();
    } else
    if (k1 != k2) {
      counts.set(k1, counts.get(k1)+counts.get(k2));
      counts.remove((int) k2);
      t--;
    }
  }


  public void encode(Integer k, Encoder enc) {
    if (k < 0 || k > t) {
      throw new ZeroMassException();
    }
    long z = (long) b2*((long) n*a2 + a1);
    if (k == t) {
      // new table
      enc.storeRegion(z - (long) a1*b2 - (long) a2*b1*t, z, z);
    } else {
      // existing table
      long l = 0;
      for (int j=0; j<k; j++) {
        l += counts.get(j);
      }
      l *= b2;
      l -= (long) k*b1;
      l *= a2;
      long h = l + (long) a2*((long) counts.get(k)*b2 - b1);
      enc.storeRegion(l, h, t);
    }
  }
  
  public Integer decode(Decoder dec) {
    long z = (long) b2*((long) n*a2 + a1);
    long r = dec.getTarget(z);
    long l = 0;
    long h = 0;
    int k = -1;
    for (int j=0; j<t; j++) {
      l = h;
      h = l + (long) a2*((long) counts.get(k)*b2 - b1);
      if (r >= l && r < h) {
        k = j;
        break;
      }
    }
    if (k != -1) {
      // existing table
      dec.loadRegion(l, h, t);
      return k;
    } else {
      // new table
      dec.loadRegion(h, t, t);
      return t;
    }
  }
  
  public void encode(Integer k, Collection<Integer> omit, Encoder enc) {
    if (k < 0 || k > t || omit.contains(k)) {
      throw new ZeroMassException();
    }
    long n0 = 0;  // total customers (after omissions)
    int t0 = 0;   // total tables (after omissions)
    int k0 = -1;  // table "k" (after omissions)
    long l0 = 0;
    for (int j=0; j<t; j++) {
      if (!omit.contains(j)) {
        if (j==k) {
          k0 = t0;
          l0 = n0;
        }
        n0+=counts.get(j);
        t0++;
      }
    }
    long z0 = (long) b2*((long) n0*a2 + a1);
    if (k0 == -1) {
      // new table
      enc.storeRegion(z0 - (long) a1*b2 - (long) a2*b1*t0, z0, z0);
    } else {
      // existing table
      // FIXME
      l0 *= b2;
      l0 -= (long) k0*b1;
      l0 *= a2;
      long h0 = l0 + (long) a2*((long) counts.get(k)*b2 - b1);
      enc.storeRegion(l0, h0, z0);
    }
  }


  public Integer decode(Collection<Integer> omit, Decoder dec) {
    long n0 = 0;  // total customers (after omissions)
    long t0 = 0;  // total tables (after omissions)
    for (int j=0; j<t; j++) {
      if (!omit.contains(j)) {
        n0+=counts.get(j);
        t0++;
      }
    }
    long z0 = (long) b2*((long) n0*a2 + a1);
    long l0 = 0;
    long h0 = 0;
    long r = dec.getTarget(z0);
    // check for existing tables
    for (int k=0; k<t; k++) {
      if (!omit.contains(k)) {
        l0 = h0;
        h0 = l0 + (long) a2*((long) b2*counts.get(k) - b1);
        if (r >= l0 && r < h0) {
          dec.loadRegion(l0, h0, z0);
          return k;
        }
      }
    }
    // otherwise: new table
    dec.loadRegion(h0, z0, z0);
    return t;
  }
  
  public Distribution<Integer> getPredictiveDistribution() {
    throw new UnsupportedOperationException();
  }
  
  
  public String toString() {
    if (n == 0) {
      return "CRP(α="+(double)a1/a2+", β="+(double)b1/b2+")";
    } else {
      return "CRP(α="+(double)a1/a2+", β="+(double)b1/b2+" | N="+n+", T="+t+", ...)";
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(a1);
    out.writeInt(a2);
    out.writeInt(b1);
    out.writeInt(b2);
    out.writeInt(n);
    out.writeInt(t);
    out.writeObject(counts);
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
    counts = (Vector<Integer>) in.readObject();
  }


  public static void main(String[] args) {
    // And here's a little demo.
    Random rnd = new Random();
    System.out.println("Chinese Restaurant Process");
    CRPI crp = new CRPI(8,1, 0,2);
    for (int i=0; i<100; i++) {
      System.out.print(crp.sample(rnd)+" ");
    }
    System.out.println();
    System.out.println(crp.n+" customers, "+crp.t+" tables.");
    System.out.println("Counts: "+crp.counts);
    System.out.println("Histogram:");
    Histogram h2 = Histogram.fromIntegers(crp, 0, 69, 9900);
    h2.print(System.out);
    System.out.println("↑ "+crp);
  }

}
