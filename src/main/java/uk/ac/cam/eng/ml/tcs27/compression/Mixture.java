/* Automated copy from build process */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.util.Random;

/** Finite mixture distribution.
  * <p>A mixture distribution is a weighted sum of component distributions
  * (over the same sample space).
  * Drawing from a mixture first samples a component number <var>k</var>
  * from the weight coefficients; then samples <var>x</var> from the
  * <var>k</var>th component distribution and returns <var>x</var>.</p> */
public class Mixture<X> extends Distribution<X> implements Codable<X> {
 
  int n = 0;
  Vector<Distribution<X>> dists;
  Discrete<Integer> mix;
  
  
  private Tuple<Tuple<Tuple<Vector<X>,Vector<Double>>,Double>,
                Tuple<Distribution<X>,Vector<X>>>
                                           defrag(Collection<X> without) {
    /** All values fetchable from finite distributions. */
    Vector<X> values = new Vector<X>();
    Vector<X> omit = new Vector<X>(without);
    /** Probability mass for each element in <var>values</var>. */
    Vector<Double> masses = new Vector<Double>();
    /** Components of this mixture which are infinite. */
    Distribution<X> inf = null;
    /** Total mass allocated in finite part. */
    double total = 0.0;
    // now step through each component...
    for (int k=0; k<n; k++) {
      Distribution<X> d = dists.get(k);
      if (d.isFinite() && d instanceof Iterable) {
        @SuppressWarnings("unchecked")
        Iterable<X> vv = (Iterable<X>) d;
        for (X v : vv) {
          if (!omit.contains(v)) {
            double m = p(v); // total mass - (omit from subsequent dists)
            omit.add(v);
            values.add(v);
            masses.add(m);
            total += m;
          }
        }
      } else
      if (inf == null) {
        // we can deal with ONE infinite distribution...
        inf = d;
      } else {
        // ...but not more than one!
        throw new RuntimeException("mixture has more than one "
                                  +"infinite component");
      }
    }
    return Tuple.of(Tuple.of(Tuple.of(values,masses),total),
                    Tuple.of(inf,omit));
  }


  public void encode(X x, Collection<X> without, Encoder ec) {
    Tuple<Tuple<Tuple<Vector<X>,Vector<Double>>,Double>,
                Tuple<Distribution<X>,Vector<X>>> stuff = null;
    stuff = defrag(without);
    // extract stuff
    Vector<X>       values = stuff.get0().get0().get0();
    Vector<Double>  masses = stuff.get0().get0().get1();
    double          total  = stuff.get0().get1();
    Distribution<X> inf    = stuff.get1().get0();
    Vector<X>       omit   = stuff.get1().get1();

    Discrete<X> fc = new Discrete<X>(values,masses,1.0/total);
    // values and masses now hold the known finite part
    // if anything is left in "infs", then that's infinite
    if (inf == null) {
      // finite component only: no problem!
      //System.err.println("#D#");
      fc.encode(x,ec);
      return;
    } else {
      // deal with infinite component
      // (leaving out values which have been taken care of!)
      boolean is_known = values.contains(x);
      //System.err.print(is_known ? "@T@" : "@F@");
      Bernoulli.booleans(total).encode(is_known, ec);
      if (is_known) {
        // code with finite component
        fc.encode(x,ec);
      } else {
        // code with infinite component
        if (inf instanceof Codable) {
          @SuppressWarnings("unchecked")
          Codable<X> ic = (Codable<X>) inf;
          ic.encode(x,omit,ec);
        } else {
          throw new RuntimeException("mixture contains non-finite, "
                                    +"non-codable component");
        }
      }
      // done
    }
  }

  public void encode(X x, Encoder ec) {
    final Collection<X> empty = Collections.emptySet();
    encode(x,empty,ec);
  }

  public X decode(Collection<X> without, Decoder dc) {
    Tuple<Tuple<Tuple<Vector<X>,Vector<Double>>,Double>,
                Tuple<Distribution<X>,Vector<X>>> stuff = null;
    stuff = defrag(without);
    // extract stuff
    Vector<X>       values = stuff.get0().get0().get0();
    Vector<Double>  masses = stuff.get0().get0().get1();
    double          total  = stuff.get0().get1();
    Distribution<X> inf    = stuff.get1().get0();
    Vector<X>       omit   = stuff.get1().get1();

    Discrete<X> fc = new Discrete<X>(values,masses,1.0/total);

    // values and masses now hold the known finite part
    // if anything is left in "infs", then that's infinite
    if (inf == null) {
      // finite component only: easy
      //System.err.println("#D#");
      return fc.decode(dc);
    } else {
      // deal with mixture of finite + infinite component
      boolean is_known = Bernoulli.booleans(total).decode(dc);
      //System.err.print(is_known ? "@T@" : "@F@");
      if (is_known) {
        // decode with finite component
        return fc.decode(dc);
      } else {
        // decode with infinite component (without the omitted points)
        if (inf instanceof Codable) {
          @SuppressWarnings("unchecked")
          Codable<X> ic = (Codable<X>) inf;
          return ic.decode(omit,dc);
        } else {
          throw new RuntimeException("mixture contains non-finite, "
                                    +"non-codable component");
        }
      }
    }
  }
  
  public X decode(Decoder dc) {
    final Collection<X> empty = Collections.emptySet();
    return decode(empty,dc);
  }

  /** Constructs a mixture distribution with component distributions
    * <var>d</var> and coefficients <var>p</var>. */
  public Mixture(Collection<? extends Distribution<X>> d,
                 Collection<Double> p) {
    this.dists = new Vector<Distribution<X>>(d);
    this.mix = Discrete.integers(new Vector<Double>(p));
    this.n = dists.size();
    // FIXME: check that p sums to one?
  }
  
  /** Constructs a mixture distribution with two components. */
  public Mixture(double p, Distribution<X> a, Distribution<X> b) {
    dists = new Vector<Distribution<X>>();
    Vector<Double> probs = new Vector<Double>();
    dists.add(a); probs.add(p);
    dists.add(b); probs.add(1.0-p);
    this.mix = Discrete.integers(0,1,probs,1.0);
    this.n = 2;
  }
  
  /** Constructs a mixture distribution with equal contributions
    * from component distributions <var>d</var>. */
  public Mixture(Collection<? extends Distribution<X>> d) {
    this.dists = new Vector<Distribution<X>>(d);
    this.n = dists.size();
    this.mix = Discrete.integers(n);
  }

  public X sample(Random rnd) {
    int k = mix.sample(rnd);
    return dists.get(k).sample(rnd);
  }

  public double mass(X x) {
    double sum = 0.0;
    for (int k=0; k<n; k++) {
      sum+=mix.mass(k) * dists.get(k).mass(x);
    }
    return sum;
  }
  
  public double logMass(X x) {
    return Math.log(mass(x));
  }
  
  public double density(X x) {
    double sum = 0.0;
    for (int k=0; k<n; k++) {
      sum+=mix.mass(k) * dists.get(k).density(x);
    }
    return sum;
  }
  
  public double logDensity(X x) {
    return Math.log(density(x));
  }
  
  public double p(X x) {
    return density(x);
  }
  public double logp(X x) {
    return logDensity(x);
  }

  /** Returns if this distribution is defined over a finite number
    * of elements.  For a mixture distribution this is true if and
    * only if all mixture components are finite. */
  public boolean isFinite() {
    boolean finite = true;
    for (int k=0; k<n; k++) {
      finite &= dists.get(k).isFinite();
    }
    return finite;
  }

  /** Converts a <code>double</code> to a <code>String</code>
    * with 4 places after the decimal dot. */
  private static String ds4(double x) {
    return Double.toString(((int) (x*10000))/10000.0);
  }

  /** Returns a String description of this mixture distribution. */
  public String toString() {
    String s = "";
    for (int k=0; k<dists.size(); k++) {
      if (s.equals("")) {
        s = ds4(mix.p(k))+"*"+dists.get(k).toString();
      } else {
        s += " + "+ds4(mix.p(k))+"*"+dists.get(k).toString();
      }
    }
    return s;
  }

  /* TODO
  public Vector<Double> responsibility(int k, Vector<X> data) {
    Vector<Double> rp = new Vector<Double>(data.size());
    for (X x : data) {
      rp.add(...);
    }
  }
  */
  
  /** Demonstrates a mixture of Gaussians. */
  public static void main(String[] args) {
    Gaussian g1 = new Gaussian(2.0, 1);
    Gaussian g2 = new Gaussian(-2.0, 1);
    Mixture<Double> mx = new Mixture<Double>(0.5,g1,g2);
    Histogram hist = Histogram.fromDoubles(mx,-5.0,5.0,61,100000);
    hist.print(System.out);
    System.out.println("↑ "+mx);
    //gauss.gnuplot();
  }

}
