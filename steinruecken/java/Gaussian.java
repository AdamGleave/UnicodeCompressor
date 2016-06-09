/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;

/** Gaussian distribution.
  * <div>The univariate <i>Gaussian</i> or <i>Normal distribution</i> is
  * a probability density defined on the real number line ℝ.
  * Its density function over points x, conditioned on mean μ and
  * variance σ², is:
  * <blockquote>
  * Gaussian(x | μ, σ²) = 1/sqrt(2πσ²) · exp((x-μ)²/σ²)
  * </blockquote></div>
  * <b>Notes:</b>
  * <ul>
  *   <li>Of all continuous distributions of given mean and variance,
  *       the Gaussian distribution is the one with maximal entropy.</li>
  *   <li>The Gaussian distribution is generalized by the
  *       <i>multivariate Gaussian distribution</i> (<code>MVG</code>).</li>
  * </ul>
  * @see MVG */
public class Gaussian extends SimpleDensity<Double> {

  /** Mean. */
  double mu;
  /** Variance. */
  double var;
  /** Standard deviation. */
  double dev;
  /** Log normalisation constant. */
  double lognorm;

  /* Other useful constants... */
  /** Entropy of a Gaussian with unit variance. */
  public static final double stdEntropy = Math.log(Math.E*Math.PI*2) / 2;
  /** Log of the square root of 2*Pi. */
  public static final double logRoot2Pi = Math.log(Math.PI*2) / 2;


  /** Constructs a new Gaussian distribution with mean 0 and variance 1. */
  public Gaussian() {
    this.mu      = 0.0;
    this.var     = 1.0;
    this.dev     = 1.0;
    this.lognorm = logRoot2Pi;
  }

  /** Constructs a new Gaussian distribution with mean <i>mu</i>
    * and variance <i>sigma_squared</i>. */
  public Gaussian(double mu, double sigma_squared) {
    this.mu      = mu;
    this.var     = sigma_squared;
    this.dev     = Math.sqrt(var);
    this.lognorm = (0.5 * Math.log(var)) + logRoot2Pi;
  }

  public String toString() {
    return "Normal(μ="+mu+",σ²="+var+")";
  }

  /** Returns the probability density at point <var>x</var>.
    * @return <code>Math.exp(logDensity(x))</code> */
  public double density(Double x) {
    return Math.exp(logDensity(x));
  }

  /** Returns the log probability density at point <var>x</var>. */
  public double logDensity(Double x) {
    return -((x-mu)*(x-mu)/(2*dev)) - lognorm;
  }

  /** Returns the mode of this distribution. */
  public Double mode() {
    return mu;
  }  

  /** Returns the mean of this distribution. */
  public Double mean() {
    return mu;
  }
  
  /** Returns the median of this distribution. */
  public Double median() {
    return mu;
  }
  
  /** Returns the variance of this distribution. */
  public Double variance() {
    return var;
  }

  /** Returns the entropy of this distribution (in nats). */
  public double entropy() {
    return 0.5*Math.log(var) + stdEntropy;
  }

  public Double sample(Random rnd) {
    return mu + (dev * Math.sin(2 * Math.PI * rnd.nextDouble())
                     * Math.sqrt(-2 * Math.log(rnd.nextDouble())));
  }

  /** Clones this Gaussian. This is computationally faster than
    * re-creating it using the constructor, because precomputed
    * constants are copied along (rather than re-calculated). */
  public Gaussian clone() {
    Gaussian g = new Gaussian();
    g.mu = this.mu;
    g.var = this.var;
    g.dev = this.dev;
    g.lognorm = this.lognorm;
    return g;
  }


}
