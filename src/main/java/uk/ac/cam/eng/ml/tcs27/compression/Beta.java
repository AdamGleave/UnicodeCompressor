/* Automated copy from build process */
/* $Id: Beta.java,v 1.18 2015/08/11 11:28:16 chris Exp $ */
import java.util.Random;
import java.util.Collection;

/** Beta distribution.
  * The Beta distribution is a probability distribution over
  * values on the real interval [0,1] with probabiliy density function:
  * <blockquote>
  *    p(<var>x</var>) = Β(<var>a</var>,<var>b</var>)⁻¹
  *    · <var>x</var><sup>(<var>a</var>-1)</sup>
  *    · (1-<var>x</var>)<sup>(<var>b</var>-1)</sup>
  * </blockquote>
  * where Β(<var>a</var>,<var>b</var>) is the <i>Beta function</i>.
  * <br>
  * <b>Connections:</b><ul>
  *   <li>The Beta distribution is conjugate prior for the
  *       <i>Bernoulli distribution</i>, <i>Binomial distribution</i>,
  *       <i>Geometric distribution</i> and
  *       <i>Negative Binomial distribution</i>.</li>
  *   <li>The Beta distribution is generalized by the
  *       <i>Dirichlet distribution</i>.</li>
  *   <li>It can be advantageous to transform the Beta distribution
  *       into logit-space, making the singularities at <var>x</var>=0
  *       and <var>x</var>=1 disappear.  See <i>LogitBeta</i> for more
  *       information.</li>
  * </ul>
  * @see Bernoulli
  * @see BetaBinomial
  * @see Binomial
  * @see Dirichlet
  * @see Geometric
  * @see NegativeBinomial
  * @see LogitBeta
  * */
public class Beta extends SimpleDensity<Double> {

  double a;
  double b;

  /** Constructs a new Beta distribution with parameters <i>alpha</i>
    * and <i>beta</i>. */
  public Beta(double alpha, double beta) {
    this.a = alpha;
    this.b = beta;
  }
  
  /** Constructs a new Beta distribution from an existing one. */
  public Beta(Beta x) {
    this.a = x.a;
    this.b = x.b;
  }
  
  /** Constructs a new Beta distribution with both parameters
    * set to <i>1/2</i>. */
  public Beta() {
    this.a = 0.5;
    this.b = 0.5;
  }

  /** Returns a copy of this Beta distribution. */
  public Beta clone() {
    return new Beta(a,b);
  }

  /** Returns the posterior Beta distribution, given prior and data. */
  public static Beta learn(Beta prior, boolean[] data) {
    Beta post = (new Beta(prior));
    post.learn(data);
    return post;
  }

  /** Learn from an array of Bernoulli observations. */
  public void learn(boolean[] d) {
    for (int k=0; k<d.length; k++) {
      if (d[k]) { a++; } else { b++; }
    }
  }
  
  /** Learn from a collection of Bernoulli observations. */
  public void learn(Collection<Boolean> data) {
    for (boolean x : data) {
      if (x) { a++; } else { b++; }
    }
  }
  
  public String toString() {
    return "Beta(α="+a+",β="+b+")";
  }

  /** Returns the probability density for point <var>x</var>.
    * If <var>x</var> lies outside the supported interval (0,1),
    * 0 is returned. */
  public double density(Double x) {
    return (x >= 0 && x <= 1.0)
            ? Math.pow(x,a-1.0) * Math.pow(1.0-x,b-1.0) / Tools.beta(a,b)
            : 0.0;
  }

  /** Returns the log probability density for point <var>x</var>.
    * If <var>x</var> lies outside the supported interval (0,1),
    * -∞ is returned. */
  public double logDensity(Double x) {
    return (x >= 0 && x <= 1.0)
      ? (a-1.0)*Math.log(x) + (b-1.0)*Math.log(1.0-x) - Tools.logBeta(a,b)
      : Double.NEGATIVE_INFINITY;
  }

  public Double mode() {
    // unique (unimodal) only for a, b > 1.
    if (a > 1.0) {
      if (b > 1.0) {
        return (a-1)/(a+b-2);
      } else {
        return 1.0; // singularity (unique)
      }
    } else {
      return 0.0;   // singularity (perhaps also 1.0)
    }
  }  

  public Double mean() {
    return a / (a+b);
  }
  
  public Double variance() {
    return (a*b) / ((a+b)*(a+b)*(a+b+1));
  }

  public double entropy() {
    if (a+b == 0) {
      return 0.0;
    } else {
      return Tools.logBeta(a,b)
           - (double) (a-1)*Tools.digamma(a)
           - (double) (b-1) *Tools.digamma(b)
           + (double) (a+b-2)*Tools.digamma(a+b);
    }
  }

  /** Marginal likelihood of this Beta distribution. */
  public double mlh(Collection<Boolean> data) {
    int aa = 0;
    int bb = 0;
    for (boolean x : data) {
      if (x) { aa++; } else { bb++; }
    }
    return Tools.beta(a+aa,b+bb) / Tools.beta(a,b);
  }
  
  /** Marginal log likelihood of this Beta distribution. */
  public double logmlh(Collection<Boolean> data) {
    int aa = 0;
    int bb = 0;
    for (boolean x : data) {
      if (x) { aa++; } else { bb++; }
    }
    return Tools.logBeta(a+aa,b+bb) - Tools.logBeta(a,b);
  }

  /** Sample from this Beta distribution.
    * Works by sampling from two Gamma distributions and combining
    * the results. */
  public Double sample(Random rnd) {
    Gamma xg = new Gamma(a,1);
    Gamma yg = new Gamma(b,1);
    double x = xg.sample(rnd);
    double y = yg.sample(rnd);
    return (x / (x+y));
  }

  public static void main(String[] args) {
    Beta b = new Beta(0.5, 0.5);
    Histogram h = Histogram.fromDoubles(b, 0.0, 1.0, 70, 2000);
    h.print(System.out);

  }
  

}
