/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;

/** The Binomial distribution.
  * <div>The Binomial distribution is parametrized by a positive
  * integer <var>n</var> and a probability <var>p</var>, and ranges
  * over the integers {0, ..., <var>n</var>}.
  * The distribution arises as the sum of <var>n</var> independent 
  * {0,1}-valued Bernoulli trials with bias <var>p</var>.</div>
  * <div><b>Notes:</b><ul>
  *    <li>The Binomial distribution is extended by the
  *      <i>Multinomial distribution</i> in the same way that the
  *      <i>Bernoulli distribution</i> is generalised by the
  *      <i>Categorical distribution</i>.</li>
  *    <li>Conjugate prior for <var>p</var> is the
  *      <i>Beta distribution</i>. The <code>BetaBinomial</code>
  *      class implements a compound form of this.</li>
  * </ul></div>
  * @see Bernoulli
  * @see Multinomial
  * @see Beta
  * @see BetaBinomial */
public class Binomial extends SimpleMass<Integer> {
  
  int n = 1;
  double p = 0.5;

  /** Creates a Binomial distribution of n fair Bernoulli trials. */
  public Binomial(int n) {
    this.n = n;
    this.p = 0.5;
  }

  /** Creates a Binomial distribution of n Bernoulli trials with bias p. */
  public Binomial(int n, double p) {
    this.n = n;
    this.p = p;
  }

  public String toString() {
    return "Binomial(n="+n+",p="+p+")";
  }

  /** Returns the probability mass for integer <var>k</var>. */
  public double mass(Integer k) {
    return Tools.choose(n,k)*Math.pow(p,k)*Math.pow(1.0-p,n-k);
  }
  
  /** Returns the log probability mass for integer <var>k</var>. */
  public double logMass(Integer k) {
    return Tools.logChoose(n,k) + k*Math.log(p) + (n-k)*Math.log(1.0-p);
  }

  /** Returns a sample from this Binomial distribution. */
  public Integer sample(Random rnd) {
    // nextDouble gives a uniform number from 0 (inclusive) to 1 (exclusive)
    double q = - Math.log(1.0 - p);
    double sum = 0.0;
    int x = 0;
    double u, e;
    while (sum <= q) {
        u = rnd.nextDouble();
        e = -Math.log(u);      // see Exponential.java
        sum += (e / (n-x));
        x += 1;
    }
    return (x-1);
  }
 
  /** Returns the mode of this distribution. */
  public Integer mode() {
    return (int) Math.floor((n+1)*p);
  }

  /** Returns the mean of this distribution. */
  public Double mean() {
    return n*p;
  }
  
  /** Returns the variance of this distribution. */
  public Double variance() {
    return n*p*(1.0-p);
  }

  /** Returns the skewness of this distribution. */
  public Double skewness() {
    return (1.0 - 2*p) / Math.sqrt(n*p*(1.0-p));
  }

  /** Returns the entropy of this distribution. */
  public double entropy() {
    double acc = 0.0;
    for (int k=0; k<=n; k++) {
      acc += p(k)*Tools.logChoose(n,k);
    }
    double mu = mean();
    return - acc - Math.log(p)*mu - Math.log(1.0-p)*mu;
  }

  /** Computes discrete probability masses summing to a given budget. */
  public long[] getDiscreteMass(long budget) {
    long sum = 0L;
    long peak = 0L;
    int  mode = -1;
    double mass = mass(0);
    long[] mm = new long[n+1];
    // compute discretized probabilities
    for (int j=0; j<=n; j++) {
      //mass = mass(j);
      // discretize (ensuring no zero mass events)
      long m = (long) (mass * (double) budget);
      if (m==0L) { m = 1L; }
      mass = mass * ((double) (n-j)*(p)) / (double) ((j+1)*(1.0 - p));
      // store
      mm[j] = m;
      sum += m;
      // update peak and mode
      if (m > peak) { peak = m; mode = j; }
    }
    if (mode == -1) {
      throw new RuntimeException("discretization failure: "+this.toString());
    }
    // FIXME: don't use peak adjustment, just distribute uniformly
    long newpeak = budget - sum + mm[mode];
    if (newpeak > 0L) {
      mm[mode] = newpeak;
    } else {
      throw new RuntimeException("discretization failure: overspent budget");
    }
    return mm;
  }

  public void encode(Integer k, Encoder ec) {
    long budget = ec.getRange();
    long[] p = getDiscreteMass(budget);
    Coding.encode(k,p,ec);
  }

  public Integer decode(Decoder dc) {
    long budget = dc.getRange();
    long[] p = getDiscreteMass(budget);
    return Coding.decode(p,dc);
  }

  public double entropy2() {
    // this term was given on Wikipedia, with a trailing +O(1/n).
    return 0.5*Math.log( 2*Math.PI*Math.E*n*p*(1.0-p) );
  }

  /** Returns if this distribution is defined on a finite set of
    * elements. For the Binomial distribution, this property is
    * always true.
    * @return true */
  public boolean isFinite() {
    return true;
  }


  /** Returns a copy of this Binomial distribution. */
  public Binomial clone() {
    return new Binomial(n,p);
  }

}
