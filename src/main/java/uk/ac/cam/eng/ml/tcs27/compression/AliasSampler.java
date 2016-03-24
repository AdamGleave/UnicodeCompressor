/* Automated copy from build process */
/* $Id: AliasSampler.java,v 1.8 2015/08/11 11:28:16 chris Exp $ */
import java.util.Random;
import java.util.Vector;
import java.util.Arrays;
import java.util.Collection;

/** Implements an alias sampler for discrete finite distributions.
  * <dl><dt><b>References</b></dt><dd><ol>
  * <li><a name="walker1977a">Alastair J. Walker.&nbsp; <i>An Efficient
  * Method for Generating Discrete Random Variables with General
  * Distributions,</i> 1977-09. In ACM Transactions on Mathematical
  Software, Vol. 3, No. 3, pages 253-256. ISSN 0098-3500.</a></li>
  * </ol></dd></dl> */
public class AliasSampler<X> implements Sampler<X>, Codable<X> {

  /** Total number of elements this distribution assigns mass to. */
  int n = 0;

  /** Values this distribution is defined over. */
  Vector<X> values;

  /** Probabilities of each value. */
  Vector<Double> p = null;

  /** Vector of alternatives, for fast sampling (method of aliases). */
  Vector<X> alt = null;

  /** Switch probabilities, for fast sampling (method of aliases). */
  double[] sw = null;

  /** Creates a new AliasSampler for a specified vector of values
    * and associated probabilities.
    * The vectors are used directly, and not copied; so discard
    * this sampler instance immediately when either vector is modified.
    * @param n total number of values
    * @param values vector of values
    * @param p probability masses for each value */
  public AliasSampler(int n, Vector<X> values, Vector<Double> p) {
    this.n = n;
    this.p = p;
    this.values = values;
    setup();
  }
  
  /** Creates a new AliasSampler from a discrete probability
    * distribution. */
  public AliasSampler(Discrete<X> dist) {
    this.n = dist.n;
    this.p = dist.p;
    this.values = dist.values;
    setup();
  }
  
  /** Initialises the alias sampler.
    * This initial setup has a time cost of O(<var>n</var>²),
    * where <var>n</var> is the number of elements in the distribution.
    * After the sampler is initialised, samples can be drawn with
    * O(1) computation time.
    * @see #sample(Random) */
  protected void setup() {
    final double errmin = 10E-9;
    if (values != null && p != null && n>0) {
      alt = new Vector<X>(values);  // same as "values", initially
      //sw  = new Vector<Double>(n);  // initialised to zero
      sw  = new double[n];          // initialised to zero
      double[] err = new double[n];
      for (int k=0; k<n; k++) {
        err[k] = p.get(k) - (1.0/n);
      }
      for (int j=0; j<n; j++) {
        double errsum = 0.0;
        int lok = -1;
        int hik = -1;
        double lo = 0.0;
        double hi = 0.0;
        // find largest pos + neg errors [hi, lo] and their indices
        for (int k=0; k<n; k++) {
          errsum += Math.abs(err[k]);
          if (err[k] < lo) {
            lok = k;
            lo = err[k];
          }
          if (err[k] > hi) {
            hik = k;
            hi = err[k];
          }
        }
        if (errsum > errmin) {
          /* set the alternative at largest negative error position to
           * the value of largest positive error position, and set the
           * switch probability sw[].  Also, update the errors of both
           * values accordingly. */
          if (lok != hik && hi > 0.0) {
            //for (int i=0; i<n; i++) {
            //  System.out.print(" "+i+":"+err[i]);
            //}
            //System.out.println();
            //System.err.println("LO="+lok+" ("+lo+")\tHI="+hik+" ("+hi+")\terr["+hik+"]="+(lo+hi) +"\tsw["+lok+"]="+(1.0+n*lo));
            alt.set(lok,values.get(hik));
            sw[lok] = 1.0 + n*lo;
            err[lok] = 0.0;
            err[hik] = lo+hi;
          }
        } else {
          // quit
          break;
        }
      }
    }
  }

  /** Samples using the method of aliases.
    * Computation time: O(1). */
  public X sample(Random rnd) {
    int k = rnd.nextInt(n);
    double u = rnd.nextDouble();
    if (u > sw[k]) {
      return alt.get(k);
    } else {
      return values.get(k);
    }
  }
  
  /** Takes <var>n</var> samples and adds them to collection
    * <code>col</code>. */
  public void sample(Random rnd, int n, Collection<X> col) {
    for (int k=0; k<n; k++) {
      col.add(sample(rnd));
    }
  }

  public void encode(X x, Encoder ec) {
    throw new UnsupportedOperationException();
  }

  public X decode(Decoder dc) {
    int k = (int) dc.getTarget(n);  // get random number between 0..(n-1)
    dc.loadRegion(k,k+1,n);
    final long max = 0x100000L;
    long db = (long) (max*sw[k]);
    long r = dc.getTarget(max);
    if (r >= db) {
      dc.loadRegion(db,max,max);
      return alt.get(k);
    } else {
      dc.loadRegion(0,db,max);
      return values.get(k);
    }
  }
  public void encode(X x, Collection<X> omit, Encoder ec) {
    throw new UnsupportedOperationException();
  }
  public X decode(Collection<X> omit, Decoder ec) {
    throw new UnsupportedOperationException();
  }

  /** Print debug information to stdout. */
  public void printStatus() {
    double z = 0.0;
    for (int k=0; k<n; k++) {
      z += sw[k];
    }
    if (z > 0.0) {
      Discrete<Integer> dsw
          = Discrete.integers(0,n-1,sw,z);
      Histogram sh = Histogram.fromIntegers(dsw,0,n-1,1000);
      System.out.println("Switch probabilities:");
      sh.print(System.out,1);
    } else {
      System.out.println("Switch probabilities sum to zero.");
    }
    System.out.println("Alternate values:");
    for (int k=0; k<n; k++) {
      System.out.print(alt.get(k)+" ");
    }
    /*
    System.out.println("Switch probs:");
    for (int k=0; k<n; k++) {
      System.out.print(sw[k]+" ");
    }
    System.out.println();
    */
  }
  
  public static void main(String[] args) {
    /*
    Discrete<Integer> di = Discrete.integers(0,4);
    di.insert(1,0.7);
    (new Histogram(di, 0, 4, 1000)).print(System.out,4);
    AliasSampler<Integer> ai = new AliasSampler<Integer>(di);
    (new Histogram(ai, 0, 4, 1000)).print(System.out,4);
    System.exit(0);
    */
    int n = 12;
    int m = 800000;
    long before = 0;
    long setup = 0;
    long after = 0;
    Random rnd = new Random();
    //NegativeBinomial nb = new NegativeBinomial(3, 0.15);
    //Discrete<Integer> nb = Discrete.integers(3,7);
    Vector<Double> aa = new Vector<Double>();
    Vector<String> v = new Vector<String>();
    v.add("A"); aa.add(0.333333);
    v.add("B"); aa.add(2.333333);
    v.add("C"); aa.add(3.5);
    v.add("D"); aa.add(3.2);
    v.add("E"); aa.add(1.5);
    v.add("F"); aa.add(0.5);
    v.add("G"); aa.add(0.2);
    v.add("H"); aa.add(0.0);
    v.add("I"); aa.add(0.333333);
    v.add("J"); aa.add(1.0);
    v.add("K"); aa.add(2.0);
    v.add("L"); aa.add(5.1);
    //Dirichlet d = new Dirichlet(n,0.8);
    //Discrete<Integer> nb = Discrete.integers(d.sample(rnd));
    //Discrete<String> nb = new Discrete<String>(v,aa,20.0);
    Discrete<Integer> nb = Discrete.integers(0,n-1,aa,20.0);
    before = System.currentTimeMillis();
    Histogram orig = Histogram.fromIntegers(nb, 0, n-1, m);
    after  = System.currentTimeMillis();
    System.out.println(m+" samples from original "+nb
                        +" distribution ("+(after-before)+"ms):");
    orig.print(System.out);
    
    Discrete<Integer> dnb = Discrete.integers(nb, 0, n-1);
    /*
    before = System.currentTimeMillis();
    dnb.sort();
    setup = System.currentTimeMillis();
    Histogram sort = new Histogram(dnb, 0, n-1, m);
    after = System.currentTimeMillis();
    System.out.println(m+" samples from a sorted discrete distribution ("
               +(setup-before)+"ms setup + "+(after-setup)+"ms sampling):");
    sort.print(System.out);
    */

    // CHECK ALIAS SAMPLING
    before = System.currentTimeMillis();
    AliasSampler<Integer> a = new AliasSampler<Integer>(n,dnb.values,dnb.p);
    //AliasSampler<String> a = new AliasSampler<String>(n,v,nb.p);
    setup = System.currentTimeMillis();
    Histogram alia = Histogram.fromIntegers(a, 0, n-1, m);
    after = System.currentTimeMillis();
    System.out.println(m+" samples from the alias sampler ("
               +(setup-before)+"ms setup + "+(after-setup)+"ms sampling):");
    alia.print(System.out);
    //a.printStatus();


    // CHECK ALIAS SAMPLING + ARITHMETIC DECODING
    /*
    before = System.currentTimeMillis();
    AliasSampler<Integer> b = new AliasSampler<Integer>(n,dnb.values,dnb.p);
    DecodeSampler<Integer> ds = new DecodeSampler<Integer>(b);
    setup = System.currentTimeMillis();
    Histogram alib = Histogram.fromIntegers(ds, 0, n-1, m);
    after = System.currentTimeMillis();
    System.out.println(m+" samples from the decode sampler ("
               +(setup-before)+"ms setup + "+(after-setup)+"ms sampling):");
    alib.print(System.out); */
    
    //a.printStatus();
    for (int k=0; k<n; k++) {
      System.out.print(v.get(k));
    }
    System.out.println();
    double z = 0.0;
    for (int k=0; k<n; k++) {
      z += a.sw[k];
    }
    if (z > 0.0) {
      Discrete<Integer> dsw
          = Discrete.integers(0,n-1,a.sw,z);
      Histogram sh = Histogram.fromIntegers(dsw,0,n-1,1000);
      sh.print(System.out,1);
    } else {
      System.out.println("Switch probabilities sum to zero.");
    }
    for (int k=0; k<n; k++) {
      System.out.print(v.get(a.alt.get(k)));
    }
    System.out.println();
  }


  

}
