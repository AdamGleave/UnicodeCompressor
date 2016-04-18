/* Automated copy from build process */
/* $Id: Binomial.java,v 1.11 2015/08/11 11:28:16 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Random;

/** The Beta-binomial compound distribution.
  * <div>The Beta-Binomial distribution is parametrized by a positive
  * integer <var>n</var>, positive real numbers <var>a</var> and <var>b</var>.
  * It ranges over the integers {0, ..., <var>n</var>}.
  * The distribution arises from integrating out the success parameter
  * p of a binomial distribution, assuming p is Beta distributed.
  * @see Binomial
  * @see Beta */
public class AdamBetaBinomial extends SimpleMass<Integer> {
  private int n;
  private double a, b;
  private double logZ;

  /** Creates a Binomial distribution of n fair Bernoulli trials. */
  public AdamBetaBinomial(int n) {
    this(n, 1.0, 1.0);
  }

  /** Creates a Binomial distribution of n Bernoulli trials with bias p. */
  public AdamBetaBinomial(int n, double a, double b) {
    this.n = n;
    this.a = a;
    this.b = b;

    /* sets normalising constant Z to Gamma(a + b)/(Gamma(a)Gamma(b)Gamma(a+B+N)) */
    this.logZ = -Tools.logBeta(this.a, this.b);
  }

  public String toString() {
    return "BetaBinomial(n="+ n +",a="+a+",b="+b+")";
  }

  /** Returns the probability mass for integer <var>k</var>. */
  public double mass(Integer k) {
    return Math.exp(logMass(k));
  }
  
  /** Returns the log probability mass for integer <var>k</var>. */
  public double logMass(Integer k) {
    double logBeta = Tools.logBeta(a + k, b + n - k);
    return logZ + Tools.logChoose(n,k) + logBeta;
  }

  /** Returns a sample from this Beta-binomial distribution. */
  public Integer sample(Random rnd) {
    Beta beta = new Beta(a, b);
    double sampleP = beta.sample(rnd);

    Binomial bin = new Binomial(n, sampleP);
    return bin.sample(rnd);
  }
 
  /** Returns the mode of this distribution. */
  public Integer mode() {
    throw new UnsupportedOperationException("Cannot compute mode for Beta-binomial");
  }

  /** Returns the mean of this distribution. */
  public Double mean() {
    return (n*a)/(a + b);
  }
  
  /** Returns the variance of this distribution. */
  public Double variance() {
    // this term was given on Wikipedia
    double numerator = n * a * b * (a + b + n);
    double denominator = Math.pow(a + b, 2) * (a + b + 1);
    return numerator / denominator;
  }

  /** Returns the skewness of this distribution. */
  public Double skewness() {
    // this term was given on Wikipedia
    double leftNumerator = (a + b + 2*n) * (b - a);
    double leftDenominator = a + b + 2;
    double left = leftNumerator / leftDenominator;

    double rightNumerator = 1 + a + b;
    double rightDenominator = n * a * b * (n + a + b);
    double right = Math.sqrt (rightNumerator / rightDenominator);

    return left * right;
  }

  /** Returns the entropy of this distribution. */
  public double entropy() {
    double entr = 0.0;
    for (int k=0; k<=n; k++) {
      double lp = logp(k);
      entr -= Math.exp(lp) * lp;
    }
    return entr;
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
      // discretize (ensuring no zero mass events)
      long m = (long) (mass * (double) budget);
      if (m==0L) { m = 1L; }
      mass = mass * ((n - j)*(a + j)) / ((j + 1)*(b + n - j - 1));
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

  /** Returns if this distribution is defined on a finite set of
    * elements. For the Beta-binomial distribution, this property is
    * always true.
    * @return true */
  public boolean isFinite() {
    return true;
  }

  /** Returns a copy of this Beta-binomial distribution. */
  public AdamBetaBinomial clone() {
    return new AdamBetaBinomial(n,a,b);
  }
}