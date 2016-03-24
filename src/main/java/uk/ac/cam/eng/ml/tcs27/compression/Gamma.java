/* Automated copy from build process */
/* $Id: Gamma.java,v 1.10 2015/08/11 11:28:16 chris Exp $ */
import java.util.Random;

/** Gamma distribution.
  * This is a distribution over the positive real line,
  * with probability density function:<br>
  * <blockquote>p(x) = x^(k-1) · e^(-x/θ)/(θ^k·Γ(k))</blockquote>
  * where Γ(k) denotes the <i>Gamma function</i>.<br>
  * The Gamma distribution is conjugate prior for the
  * <i>Poisson distribution</i>, <i>Exponential distribution</i>,
  * and for the variance of the <i>Gaussian distribution</i>.<br>
  * @see Poisson
  * @see Exponential
  * @see Gaussian
  * @see Tools#gamma(double) */
public class Gamma extends SimpleDensity<Double> {

  double k;
  double t;

  /** Constructs a new Gamma distribution with parameters <i>k</i>
    * and <i>theta</i>. */
  public Gamma(double k, double theta) {
    this.k = k;
    this.t = theta;
  }

  /** Returns a String representation of this Gamma distribution. */
  public String toString() {
    return "Gamma(k="+k+",θ="+t+")";
  }

  /** Returns the probability density at point <var>x</var>. */
  public double density(Double x) {
    //return (t*Math.exp(-t*x) * Math.pow(t*x, k-1) / Tools.gamma(k));
    return Math.pow(x,k-1) * (Math.exp(-x/t)/Math.pow(t,k)) / Tools.gamma(k);
  }

  /** Returns the log probability density at point <var>x</var>. */
  public double logDensity(Double x) {
    return (k-1.0)*Math.log(x) - x/t - k*Math.log(t) - Tools.logGamma(k);
  }

  /** Returns the mode of this Gamma distribution. */
  public Double mode() {
    if (k >= 1.0) {
      return (k-1)*t;
    } else {
      return 0.0;   // singularity
    }
  }  

  /** Returns the mean of this Gamma distribution. */
  public Double mean() {
    return k*t;
  }
  
  /** Returns the variance of this Gamma distribution. */
  public Double variance() {
    return k*t*t;
  }

  /** Returns the entropy of this Gamma distribution. */
  public double entropy() {
    return k + Math.log(t) + Tools.logGamma(k) + (1.0-k)*Tools.digamma(k);
  }

  /** Sample from this Gamma distribution.
    * Code adapted from the MIT blog distribution. */
  public Double sample(Random rnd) {
    boolean accept = false;
    if (k >= 1) {
      // Cheng's algorithm
      double b = (k - Math.log(4)); 
      double c = (k + Math.sqrt(2*k - 1));
      double lam = Math.sqrt(2*k - 1);
      double cheng = (1 + Math.log(4.5));
      double u, v, x, y, z, r;
      do {
        u = rnd.nextDouble();
        v = rnd.nextDouble();
        y = ((1 / lam) * Math.log(v / (1 - v)));
        x = (k * Math.exp(y));
        z = (u * v * v); 
        r = (b + (c * y) - x); 
        if ((r >= ((4.5 * z) - cheng)) || (r >= Math.log(z))) {
          accept = true;
        }
      } while (!accept); 
      return (x / t);
    } else {
      // Weibull algorithm
      double c = (1 / k); 
      double d = ((1 - k) * Math.pow(k, (k / (1 - k))));
      double u, v, z, e, x;
      do {
        u = rnd.nextDouble();
        v = rnd.nextDouble();
        z = -Math.log(u); //generating random exponential variates
        e = -Math.log(v);
        x = Math.pow(z, c); 
        if ((z + e) >= (d + x)) {
            accept = true;
        }
      } while (!accept);
      return (x / t);
    }
  }

}
