/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;
import java.util.Vector;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/** Implements finite discrete probability distributions.
  * <p>This distribution is parametrised by a finite collection
  * of <var>n</var> elements (of any type X) and an associated vector of 
  * <var>n</var> probability masses (real numbers, summing to 1).
  * If a probability vector is not provided, a uniform
  * distribution over the elements is assumed.</p>
  * <p>This implementation is very general, in the sense any distribution
  * can be represented which has all mass on a finite number of points.
  * Points which are missing in the collection are given zero
  * probability mass.</p>
  * <b>Connections with other distributions:</b>
  * <ul>
  *     <li>When <var>n</var>=2, this distribution is equivalent to
  *         a <i>Bernoulli distribution</i>.</li>
  *     <li>The <i>Categorical distribution</i> is a discrete distribution
  *         with particular restrictions on X.</li>
  *     <li>A possible conjugate prior for the probability masses is
  *         the <i>Dirichlet distribution</i>. A compound form (with
  *         probability vectors integrated out) is implemented in the
  *         <i>Dirichlet-Discrete</i> compound distribution (DDC).</li>
  * </ul>
  * <b>Notes of caution:</b>
  * <ul><li>The vector of probability masses must sum to 1, but no strict
  *         checks are implemented. The sampling algorithm will operate
  *         correctly even when the total sum is less than 1 (by implicitly
  *         renormalising, at a performance cost).
  *         However, if the sum exceeds 1, samples will be biased.</li>
  *     <li>The collection of elements may not contain duplicate elements,
  *         but this isn't enforced.  If duplicates are present, sampling
  *         will operate correctly, but calls to <code>p(x)</code> and
  *         <code>logp(x)</code> may not (by understating the true
  *         probability mass).</li>
  * </ul>
  * @see Bernoulli
  * @see Categorical
  * @see Dirichlet
  * @see DDC */
public class Discrete<X> extends Distribution<X> implements Externalizable,
                                                            Codable<X>,
                                                            Iterable<X> {

  static final long serialVersionUID = 1858603243071798894L;

  /** Total number of elements this distribution assigns mass to. */
  int n = 0;
  /** Values this distribution is defined over. */
  Vector<X> values;
  /** Probabilities of each value. */
  Vector<Double> p;
  /** A cached String representation of the value set. */
  protected String vals = null;

  /** An alias sampler for optional "turbo mode" sampling. */
  AliasSampler<X> a = null;


  public void encode(X x, Encoder ec) {
    if (p == null) {
      // implicit uniform
      int i = values.indexOf(x);
      if (i == -1) {
        throw new IllegalArgumentException("unknown element: "+x);
      }
      ec.storeRegion(i,i+1,n);
      return;
    } else {
      // general case (with probability masses given in p)
      long t = ec.getRange();
      /* To ensure each element has an interval width of at least 1,
       * we'll allocate that amount first.  If n > t, we're screwed. */
      long te = t - n;  // allows space for the uniform component
      double cp = 0.0;
      for (int k=0; k < n; k++) {
        double pp = indexp(k);
        X v = values.get(k);
        if (v.equals(x)) {
          long l = k+ (long) (cp*te);
          long h = k+1+ (long) ((cp+pp)*te);
          ec.storeRegion(l,h,t);
          return;
        } else {
          cp += pp;
        }
      }
    }
    // we only get here if element x wasn't found...
    throw new IllegalArgumentException("element "+x+" (index "+values.indexOf(x)+" in 0.."+n+") not found.");
  }
  

  public void encode(X x, Collection<X> without, Encoder ec) {
    if (p == null) {
      // implicit uniform
      long rtotal  = 0;
      long rbefore = 0;
      int i = values.indexOf(x);
      if (i == -1) {
        throw new IllegalArgumentException("unknown element: "+x);
      }
      // count total number of elements left
      for (int k=0; k<n; k++) {
        X v = values.get(k);
        if (without.contains(v)) {
          // count how many of our symbols are in the "to omit" set
          rtotal++;
          if (k < i) {
            // and how many of those occur before "x"
            rbefore++;
          } else
          if (k == i) {
            // "x" itself had better not be omitted
            throw new IllegalArgumentException("attempt to encode an "
                                              +"excluded element");
          }
        }
      }
      ec.storeRegion(i-rbefore,i-rbefore+1,n-rtotal);
      return;
    } else {
      // TODO: implement this
      throw new UnsupportedOperationException();
    }
  }

  public X decode(Decoder dc) {
    if (p == null) {
      long j = dc.getTarget(n);
      dc.loadRegion(j,j+1,n);
      return values.get((int) j);
    } else {
      long t = dc.getRange();
      long i = dc.getTarget(t);
      long te = t - n;  // allows space for the uniform component
      double cp = 0.0;
      for (int k=0; k < n; k++) {
        double pp = indexp(k);
        long l = k + (long) (cp*te);
        long h = k+1 + (long) ((cp+pp)*te);
        if (i < h && i >= l) {
          //System.err.println(p);
          dc.loadRegion(l,h,t);
          return values.get(k);
        }
        cp += pp;
      }
    }
    // we got here if no region was found
    // -- maybe we zoomed into left over space?
    throw new IllegalArgumentException("lost in space");
  }
  
  public X decode(Collection<X> without, Decoder dc) {
    if (p == null) {
      // implicit uniform
      long rtotal  = 0;
      long rbefore = 0;
      for (X v : values) {
        if (without.contains(v)) {
          // count how many of our symbols are in the "to omit" set
          rtotal++;
        }
      }
      if (rtotal == 0) {
        // fall back on simple method
        return decode(dc);
      } else
      if (n == rtotal) {
        throw new IllegalArgumentException("attempt to decode a "
                                          +"zero-probability event");
      } else {
        // this could probably be done more efficiently...
        long i = dc.getTarget(n-rtotal);
        long k = 0;
        int j = 0;  // value index to be found
        X v = values.get(j);
        while (without.contains(v)) {
          j++;
          v = values.get(j);
        }
        while (k < i) {
          k++; j++;
          v = values.get(j);
          while (without.contains(v)) {
            j++;
            v = values.get(j);
          }
        }
        dc.loadRegion(i,i+1,n-rtotal);
        return v;
      }
    } else {
      // TODO: implement this
      throw new UnsupportedOperationException();
    }
  }
  
  


  /** Initialises the optional high speed sampler.
    * This instantiates an internal AliasSampler for this distribution.
    * @see AliasSampler
    * @see #sampleFast(Random)
    * @see #disableFastSampling() */
  public void enableFastSampling() {
    if (values != null && p != null && n>0) {
      // Note that pointers are shared.
      this.a = new AliasSampler<X>(n,values,p);
    }
  }

  /** Samples from this distribution, using the method of aliases.
    * Before this method can be used, the fast sampler must be
    * initialised using <code>enableFastSampling()</code>.
    * The initialisation has to be done only once.
    * @see #enableFastSampling() */
  public X sampleFast(Random rnd) {
    return a.sample(rnd);
  }
  
  /** Deactivates the optional high speed sampler.
    * @see #enableFastSampling()
    * @see #sampleFast(Random) */
  public void disableFastSampling() {
    this.a = null;
  }

  /** Empty constructor, for serialization. */
  public Discrete() {
  }

  /** Creates a discrete uniform distribution over the elements
    * of the specified collection.
    * The collection <var>col</var> must have no duplicate
    * elements, and at least one element. The collection is
    * copied into a vector. */
  public Discrete(Collection<X> col) {
    this.n = col.size();
    assert (n >= 1);
    this.values = new Vector<X>(col);
    this.p = null; // automatic uniform distribution
  }
  
  /** Creates a discrete uniform distribution over the elements
    * of the specified iterable object.
    * The elements from <var>xx</var> are copied into a vector,
    * must have no duplicates and at least one element. */
  public Discrete(Iterable<X> xx) {
    this.values = new Vector<X>();
    this.n = 0;
    for (X x : xx) {
      this.values.add(x);
      this.n++;
    }
    this.p = null; // automatic uniform distribution
  }
  
  /** Creates a discrete uniform distribution over the elements
    * in the specified array.
    * The array must have no duplicate elements, and at least
    * one element. The array is copied into a vector. */
  public Discrete(X[] array) {
    this.n = array.length;
    assert (n >= 1);
    this.values = new Vector<X>(Arrays.asList(array));
    this.p = null;  // automatic uniform distribution
  }
  
  /** Creates a discrete uniform distribution over the elements
    * of a shared vector.  This vector is not cloned.
    * @param v shared vector of elements */
  public Discrete(Vector<X> v) {
    this.n = v.size();
    assert (n >= 1);
    this.values = v;
    this.p = null; // automatic uniform distribution
  }
  
  /** Creates a discrete distribution by cloning another
    * discrete distribution. Internal vectors are cloned (not linked). */
  public Discrete(Discrete<X> dist) {
    this.n = dist.n;
    this.values = new Vector<X>(dist.values);
    if (dist.p != null) {
      this.p = new Vector<Double>(dist.p);
    } else {
      this.p = null;
    }
    /* AliasSampler for the fast sampling method -- this sampler is never
     * modified in place, so it's safe to link it instead of copying. */
    this.a = dist.a;
    assert(n >= 1);
    assert(p.size() == n);
  }
  
  /** Creates a discrete distribution from an arbitrary finite
    * distribution and a complete list of its elements.
    * The probability mass from <var>dist</var> must sum to 1 over
    * the elements in <var>v</var>.
    * The vector <var>v</var> is not cloned.
    * @param dist original distribution
    * @param v shared vector of elements */
  public Discrete(Distribution<X> dist, Vector<X> v) {
    this.n = v.size();
    assert (n >= 1);
    this.values = v;
    this.p = new Vector<Double>(n);
    for (X x : v) {
      p.add(dist.mass(x));
    }
  }
 
 
  /** Creates a discrete distribution from an arbitrary finite
    * distribution and a way to iterate over its elements.
    * The probability mass from <var>dist</var> must sum to 1 over
    * the elements in <var>col</var>.
    * It's the caller's responsibility to ensure that the iterable
    * collection is finite.
    * @param dist original distribution
    * @param col an Iterable over the elements of the distribution */
  public Discrete(Distribution<X> dist, Iterable<X> col) {
    this.values = new Vector<X>();
    this.p = new Vector<Double>();
    this.n = 0;
    for (X x : col) {
      this.values.add(x);
      this.p.add(dist.mass(x));
      this.n++;
    }
  }
  
  /** Creates a discrete distribution over shared elements <var>v</var>
    * with probabilities <var>p</var>. Note: the vectors are linked,
    * not copied, so watch out for external modification!
    * @param v values
    * @param p probability masses */
  public Discrete(Vector<X> v, Vector<Double> p) {
    this.n = v.size();
    assert(n >= 1);
    assert(p.size() == n);
    this.values = v;
    this.p = p;
    // TODO: add safety check?
  }
  
  /** Creates a discrete distribution over shared elements <var>v</var>
    * with weights <var>w</var> and normalizing constant <var>z</var>.
    * Note: the vectors are linked, not copied, so watch out for external
    * modification.
    * @param v values
    * @param w weights (unnormalized)
    * @param z normalizing constant (sum of all weights) */
  public Discrete(Vector<X> v, Vector<Double> w, double z) {
    this.n = v.size();
    this.values = v;
    this.p = new Vector<Double>(n);
    assert(n >= 1);
    assert(p.size() == n);
    for (double d : w) {
      this.p.add(d/z);
    }
    // TODO: add safety check?
  }
  
  /** Creates a discrete distribution over shared elements <var>v</var>
    * with probabilities <var>p</var>. <br>
    * <b>Note:</b> The vector is linked, not copied. */
  public Discrete(Vector<X> v, double[] p) {
    this.n = v.size();
    assert(n >= 1);
    assert(p.length == n);
    this.values = v;
    this.p = new Vector<Double>(n);
    for (double d : p) {
      this.p.add(d);
    }
    // TODO: add safety check?
  }
  
  /** Creates a discrete distribution over elements <var>v</var>
    * with probabilities <var>p</var>. */
  public Discrete(X[] v, double[] p) {
    this.n = v.length;
    assert(n >= 1);
    assert(p.length == n);
    this.values = new Vector<X>(Arrays.asList(v));
    this.p = new Vector<Double>(n);
    for (double d : p) {
      this.p.add(d);
    }
    // TODO: add safety check?
  }

  /** Creates a discrete distribution over collection <var>col</var>
    * with probabilities initialised to <var>p</var>. */
  public Discrete(Collection<X> col, Collection<Double> p) {
    this.n = col.size();
    assert(n >= 1);
    assert(p.size() == n);
    this.values = new Vector<X>(col);
    this.p = new Vector<Double>(p);
    // TODO: add safety check
    // verify things sum to one?
  }
  
  /** Creates a discrete distribution over values <var>v</var>
    * with associated probabilities <var>p</var>. */
  public Discrete(Iterable<X> v, Iterable<Double> p) {
    this.values = new Vector<X>();
    this.p = new Vector<Double>();
    this.n = 0;
    for (X x : v) {
      this.values.add(x);
      this.n++;
    }
    for (double d : p) {
      this.p.add(d);
    }
    // TODO: add safety check?
  }


  /** Creates a discrete distribution over values <var>v</var>
    * with associated weights <var>w</var> and normalizing constant
    * <var>z</var>.
    * @param v values
    * @param w unnormalized weights
    * @param z normalizing constant (sum of all weights) */
  public Discrete(Iterable<X> v, Iterable<Double> w, double z) {
    this.values = new Vector<X>();
    this.p = new Vector<Double>();
    this.n = 0;
    for (X x : v) {
      this.values.add(x);
      this.n++;
    }
    for (double d : w) {
      this.p.add(d/z);
    }
    // TODO: add safety check?
  }
  
  /** Creates a discrete distribution from a map.
    * @param map map from values to probabilities */
  public Discrete(Map<X,Double> map) {
    this.n = map.size();
    if (n == 0) {
      throw new IllegalArgumentException("Map has no elements");
    }
    this.values = new Vector<X>(map.keySet());
    this.p = new Vector<Double>();
    for (X x : values) {
      p.add(map.get(x));
    }
    // TODO: add safety check?
  }
  
  /** Creates a discrete distribution from a map.
    * @param map map from values to unnormalized weights
    * @param z normalizing constant (sum of all weights) */
  public Discrete(Map<X,Double> map, double z) {
    this.n = map.size();
    if (n == 0) {
      throw new IllegalArgumentException("Map has no elements");
    }
    this.values = new Vector<X>(map.keySet());
    this.p = new Vector<Double>();
    for (X x : values) {
      p.add(map.get(x)/z);
    }
    // TODO: add safety check?
  }
  
  /** Creates a discrete uniform distribution over the integers
    * from <var>a</var> to <var>b</var>, inclusive. */
  public static Discrete<Integer> integers(int a, int b) {
    int n = b-a+1;
    assert (n >= 1);
    Vector<Integer> ints = new Vector<Integer>();
    for (int k=a; k<=b; k++) {
      ints.add(k);
    }
    Discrete<Integer> dist = new Discrete<Integer>(ints);
    // add a sensible string representation
    dist.vals = intRangeString(a,b);
    return dist;
  }

  /** Creates a discrete uniform distribution over the first
    * <var>n</var> integers, starting from 0. */
  public static Discrete<Integer> integers(int n) {
    return integers(0,n-1);
  }
  
  /** Creates a discrete distribution over the integers
    * from <var>a</var> to <var>b</var> (inclusive), using
    * weights <var>w</var> and normalizing constant <var>z</var>.
    * @param a lowest integer
    * @param b highest integer
    * @param w weights (unnormalized)
    * @param z normalizing constant (sum of all weights) */
  public static Discrete<Integer> integers(int a, int b,
                  Vector<Double> w, double z) {
    int n = b-a+1;
    assert (n >= 1);
    Vector<Integer> ints = new Vector<Integer>();
    Vector<Double> probs = new Vector<Double>();
    for (int k=a; k<=b; k++) {
      ints.add(k);
      probs.add(w.get(k-a) / z);
    }
    return new Discrete<Integer>(ints,probs);
  }
  
  /** Creates a discrete distribution over the integers
    * from <var>a</var> to <var>b</var> (inclusive), using
    * weights <var>w</var> and normalising
    * constant <var>z</var>.
    * @param a lowest integer
    * @param b highest integer
    * @param w weights (unnormalized)
    * @param z normalizing constant (sum of all weights) */
  public static Discrete<Integer> integers(int a, int b,
                  double[] w, double z) {
    int n = b-a+1;
    assert (n >= 1);
    Vector<Integer> ints = new Vector<Integer>();
    Vector<Double> probs = new Vector<Double>();
    for (int k=a; k<=b; k++) {
      ints.add(k);
      probs.add(w[k-a] / z);
    }
    return new Discrete<Integer>(ints,probs);
  }
 

  /** Creates a discrete distribution with probabilities <var>p</var>,
    * over integers from 0 to <code>size(p)-1</code>. */
  public static Discrete<Integer> integers(Vector<Double> p) {
    int n = p.size();
    assert (n >= 1);
    Vector<Integer> ints = new Vector<Integer>();
    for (int k=0; k<n; k++) {
      ints.add(k);
    }
    Discrete<Integer> dist = new Discrete<Integer>(ints,p);
    dist.vals = intRangeString(0,n-1);
    return dist;
  }
  
  /** Creates a discrete distribution from a specified subset of a
    * distribution over integers.
    * @param dist distribution of integers
    * @param low lowest index (inclusive)
    * @param high highest index (inclusive)
    * @throws IllegalArgumentException if the specified subset has
    *         zero or negative probability mass. */
  public static Discrete<Integer> integers(Distribution<Integer> dist,
                                                       int low, int high) {
    int n = high-low+1;
    Vector<Double> probs = new Vector<Double>();
    Vector<Integer> vals = new Vector<Integer>();
    double z = 0.0;
    for (int k=low; k<=high; k++) {
      vals.add(k);
      double m = dist.mass(k);
      probs.add(m);
      z+=m;
    }
    if (z > 0.0) {
      for (int k=0; k<n; k++) {
        probs.set(k,probs.get(k)/z);
      }
      return new Discrete<Integer>(vals,probs);
    } else {
      throw new IllegalArgumentException("subset has zero or negative probability mass: "+z);
    }
  }
  
  /** Creates uniform distribution over characters from a char array. */
  public static Discrete<Character> chars(char[] symbols) {
    Vector<Character> chars = new Vector<Character>();
    for (int k=0; k<symbols.length; k++) {
      chars.add(symbols[k]);
    }
    Discrete<Character> dist = new Discrete<Character>(chars);
    return dist;
  }
  
  /** Creates uniform distribution over characters from a String. */
  public static Discrete<Character> chars(String symbols) {
    Vector<Character> chars = new Vector<Character>();
    for (int k=0; k<symbols.length(); k++) {
      chars.add(symbols.charAt(k));
    }
    Discrete<Character> dist = new Discrete<Character>(chars);
    return dist;
  }


  /** Returns a cloned copy of this distribution.
    * @see #Discrete(Discrete) */
  public Discrete<X> clone() {
    return new Discrete<X>(this);
  }

  /** Returns the number of elements on which this discrete distribution is defined. */
  public int size() {
    return n;
  }

  /** Returns an iterator over values. */
  public Iterator<X> iterator() {
    return values.iterator();
  }

  /** Returns if this distribution is defined over a finite set of
    * elements.  For the Discrete class, this is always true.
    * @return true */
  public boolean isFinite() {
    return true;
  }

  /** Returns the sum of all probability mass, for testing. */
  double getSum() {
    if (p != null) {
      // explicit probability masses: sum them
      double sum = 0.0;
      for (double d : p) {
        sum+=d;
      }
      return sum;
    } else {
      // implicit uniform distribution: return 1
      return 1.0;
    }
  }

  /** Sets probability vector <var>pp</var>. <br>
    * <b>Warning</b>: No normalisation check and no cloning is done,
    * so use this with caution. */
  public void set(Vector<Double> pp) {
    assert (pp.size()== n);
    this.p = pp;
  }
  
  /** Creates an explicit and uniform probability vector.
    * This method is called internally when an explicit table is
    * needed, but absent.
    * @see #set(Vector)
    * @see #setp(int,double) */
  protected void setUniformTable() {
    this.p = new Vector<Double>(n);
    for (int j=0; j<n; j++) {
      p.add(1.0/n);
    }
  }
  
  /** Modifies the <var>k</var>th element by assigning it
    * probability <var>q</var> and renormalising all others.
    * The probabilities of all other elements are normalised
    * to sum to (1-<var>q</var>).
    * @see #insert(Object,double)
    * @throws IllegalArgumentException in some (but not all) bad situations */
  public void setp(int k, double q) throws IllegalArgumentException {
    // FIXME: this method should be called "setMass"
    if (k >= 0 && k < n) {
      if (indexp(k) != q) { // only proceed if we're actually making a change
        if (p == null) {
          // create explicit probability table, if missing
          setUniformTable();
        }
        /** Old probability mass of all values except x. */
        double oldsum = 1.0 - p.get(k);
        /** New probability mass of all values except x. */
        double newsum = 1.0 - q;
        /* Complain if no probability mass is left. */
        if (oldsum == 0.0) {
          throw new IllegalArgumentException();
        }
        for (int i=0; i<n; i++) {
          if (i != k) {
            p.set(i, (p.get(i) / oldsum) * newsum);
          } else {
            p.set(i, q);  // set i (resp k) to probability q
          }
        }
        /* we also have to clear any auxiliary data kept for the
         * fast sampling method. */
        disableFastSampling();
        // values didn't change -- just the probabilities
      } else {
        // k is out of range
        throw new IllegalArgumentException();
      }
    }
  }

  /** Inserts or modifies element <var>x</var> to have
    * probability <var>q</var>.
    * The probabilities of all other values are normalised
    * to sum to (1-<var>q</var>).
    * @see #setp(int,double)
    * @throws IllegalArgumentException in some (but not all) bad situations */
  public void insert(X x, double q) throws IllegalArgumentException {
    int k = values.indexOf(x);
    //if (p == null) {
    //  setUniformTable();
    //}
    if (k != -1) {
      setp(k,q);
    } else {
      if (p == null) {
        if (q == 1.0/(n+1)) {
          /* In the rare case that the element we're inserting
           * has probability identical to what it would have
           * in a uniform distribution, we can avoid creating
           * an explicit table. */
          values.add(x);
        } else {
          /* Otherwise, we're less lucky and must create an
           * explicit table. */
          double newsum = 1.0 - q;
          this.p = new Vector<Double>(n);
          for (int j=0; j<n; j++) {
            p.add(newsum/n);
          }
          /* Add new element and probability mass. */
          values.add(x);
          p.add(q);
        }
      } else {
        /* A table already exists, and we need to adjust it. */
        double newsum = 1.0 - q;
        for (int i=0; i<n; i++) {
          p.set(i, p.get(i) * newsum);
        }
        /* Add new element and probability mass */
        values.add(x);
        p.add(q);
      }
      /* In all cases we're incrementing n and clearing "vals". */
      vals = null;
      n++;
      /* we also have to clear any auxiliary data kept for the
       * fast sampling method. */
       disableFastSampling();
    }
  }

  /** Converts a <code>double</code> to a <code>String</code>
    * with 4 places after the decimal dot. */
  protected static String ds4(double x) {
    return Double.toString(((int) (x*10000))/10000.0);
  }


  /** Returns an approximate String representation of this
    * distribution.  Descriptions may be incomplete for distributions
    * which are complicated or have a domain of many elements.
    * Elements are omitted for the sake of readability. */
  public String toString() {
    if (p != null) {
      if (values.size() > 0) {
        String pars = values.get(0)+":"+ds4(p.get(0));
        if (n < 6) {
          for (int k=1; k<n; k++) {
            pars+=", "+values.get(k)+":"+ds4(p.get(k));
          }
        } else {
          for (int k=1; k<3; k++) {
            pars+=", "+values.get(k)+":"+ds4(p.get(k));
          }
          pars+=", ..., "+values.get(n-1)+":"+ds4(p.get(n-1));
        }
        return "Discrete("+pars+")";
      } else {
        return "ImproperDiscrete(no elements)";
      }
    } else {
      if (vals != null) {
        return "Uniform("+vals+")";
      } else {
        String s = "{"+values.get(0);
        for (int k=1; k<n; k++) {
          s+=", "+values.get(k);
        }
        s+="}";
        vals=s;
        return "Uniform("+vals+")";
      }
    }
  }

  /** Returns the probability mass of the <var>k</var>th element.
    * If <var>k</var> is out of bounds, 0 is returned. */
  public double indexp(int k) {
    if (k >= 0 && k < n) {
      if (p != null) {
        return p.get(k);
      } else {
        return 1.0 / n;
      }
    } else {
      return 0.0;
    }
  }
  
  /** Returns the log probability mass of the <var>k</var>th element.
    * If <var>k</var> is out of bounds, -∞ is returned. */
  public double indexlogp(int k) {
    if (k >= 0 && k < n) {
      if (p != null) {
        return Math.log(p.get(k));
      } else {
        return -Math.log(n);
      }
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }
  
  /** Returns the probability mass of <var>x</var>.
    * Calls <code>mass(x)</code>. */
  public double p(X x) {
    return mass(x);
  }
  
  /** Returns the log probability mass of <var>x</var>.
    * Calls <code>logMass(x)</code>. */
  public double logp(X x) {
    return logMass(x);
  }
  


  /** Returns the probability mass of <var>x</var>.
    * If <var>x</var> is not a known value, 0 is returned. */
  public double mass(X x) {
    int k = values.indexOf(x);
    if (k != -1) {
      if (p != null) {
        return p.get(k);
      } else {
        return 1.0 / n;
      }
    } else {
      return 0.0;
    }
  }
 
  /** Returns the log probability mass of <var>x</var>.
    * If <var>x</var> is not a known value, -∞ is returned. */
  public double logMass(X x) {
    int k = values.indexOf(x);
    if (k != -1) {
      if (p != null) {
        return Math.log(p.get(k));
      } else {
        return -Math.log(n);
      }
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }

  /** Returns the total mass of elements in <var>col</var>. */
  public double totalMass(Iterable<X> col) {
    if (p != null) {
      double mass = 0.0;
      for (X x : col) {
        int k = values.indexOf(x);
        if (k != -1) {
          mass += p.get(k);
        }
      }
      return mass;
    } else {
      int count = 0;
      for (X x : col) {
        if (values.contains(x)) {
          count++;
        }
      }
      return (double) count/n;
    }
  }

  /** Returns the probability density at point <var>x</var>.
    * The probability density is positive infinity for known values,
    * and zero everywhere else.
    * Since this is a discrete distribution, you may want to use
    * probability mass instead.
    * @see #mass(Object) */
  public double density(X x) {
    int k = values.indexOf(x);
    if (k != -1) {
      return Double.POSITIVE_INFINITY;
    } else {
      return 0.0;
    }
  }
  
  /** Returns the probability density at point <var>x</var>.
    * The probability density is positive infinity for known values,
    * and negative infinity everywhere else.
    * Since this is a discrete distribution, you may want to use
    * log probability mass instead.
    * @see #logMass(Object) */
  public double logDensity(X x) {
    int k = values.indexOf(x);
    if (k != -1) {
      return Double.POSITIVE_INFINITY;
    } else {
      return Double.NEGATIVE_INFINITY;
    }
  }
  
  /** Samples from this discrete distribution.
    * Uses fast sampling if available, otherwise samples by
    * calling <code>Samplers.sampleIndex()</code>.
    * Note: the fast sampler is not active by default, but can be
    * initialised by calling <code>enableFastSampling()</code>,
    * which has runtime cost of O(<var>n</var>²), where <var>n</var>
    * is the number of elements in the distribution.
    * The fast sampler has runtime cost O(1), while the standard
    * sampler works has runtime cost O(<var>n</var>).
    * @see #sampleFast(Random)
    * @see #enableFastSampling()
    * @see Samplers#sampleIndex(Random,Iterable) */
  public X sample(Random rnd) {
    if (p != null) {
      if (a != null) {
        return sampleFast(rnd);
      } else {
        int k = Samplers.sampleIndex(rnd,p);
        return values.get(k);
      }
    } else {
      return Samplers.uniform(rnd, values);
    }
  }

  /** Returns a vector of <var>k</var> unique values
    * sampled from this distribution.
    * @param k number of unique samples to return
    * @throws IllegalArgumentException when k is out of bounds,
    *         or if the distribution has mass on less than k
    *         values. */
  public Vector<X> sampleUnique(Random rnd, int k)
                                    throws IllegalArgumentException {
    if (k > n || k < 0) {
      throw new IllegalArgumentException();
    } else {
      Vector<X> res = new Vector<X>();
      Discrete<X> copy = this.clone();
      while (k > 0) {
        X sample = copy.sample(rnd);
	res.add(sample);
	copy.insert(sample,0.0);  // zero its probability
	k--;
      }
      return res;
    }
  }
  
  /** Returns the entropy of this distribution, in nats.
    * Calls <code>Tools.entropy(pp)</code>.
    * @see Tools#entropy(Collection) */
  public double entropy() {
    return Tools.entropy(p);
  }

  /** Returns the mode of this distribution.
    * If there are several modes, the last is returned. */
  public X mode() {
    X x = null;
    double peak = 0.0;
    for (int k=0; k < n; k++) {
      double r = p.get(k);
      if (r >= peak) {
        peak = r;
        x = values.get(k);
      }
    }
    return x;
  }

  /** Re-arranges the elements in order of probability mass. */
  public void sort() {
    if (p == null) {
      // implicit uniform distributions are already sorted
      return;
    } else {
      // otherwise, do a manual merge sort
      sort(0,n-1);
      // our shared values may have changed, so invalidate the AliasSampler
      this.a = null;
    }
  }

  /** Merge-sorts elements and probabilities in descending order of
    * probability mass.
    * This runs a basic merge sort, modifying <code>values</code> and
    * <code>p</code> in place.
    * @see #values
    * @see #p
    * @see #sort(void) */
  private void sort(int k0, int k1) {
    if (k0 >= k1) {
      return;
    } else
    if (k0 == k1-1) {
      if (p.get(k0) < p.get(k1)) {
        X xtmp = values.get(k0);
        values.set(k0,values.get(k1));
        values.set(k1,xtmp);
        double mtmp = p.get(k0);
        p.set(k0,p.get(k1));
        p.set(k1,mtmp);
      }
      // we're done
      return;
    } else {
      int m = k0 + (k1-k0)/2;
      sort(k0,m);
      sort(m,k1);
      // now merge
      int lo = k0;
      int hi = m;
      double phi = p.get(hi);
      double plo = p.get(lo);
      while (lo < k1) {
        if (plo >= phi) {
          // take, advance lo
          lo++;
          plo = p.get(lo);
        } else
        if (plo < phi) {
          // swap in, advance hi
          X xtmp = values.get(lo);
          values.set(lo,values.get(hi));
          values.set(hi,xtmp);
          p.set(lo,phi);
          p.set(hi,plo);
          lo++;  plo = p.get(lo);
          hi++;  phi = p.get(hi);
        }
      }
      // done!
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

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(n);
    out.writeObject(p);
    out.writeObject(values);
    out.writeObject(vals);
    out.writeObject(a);
  }

  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
                                                  ClassNotFoundException {
    n = in.readInt();
    p = (Vector<Double>) in.readObject();
    values = (Vector<X>) in.readObject();
    vals = (String) in.readObject();
    a = (AliasSampler<X>) in.readObject();
  }


}
