/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;
import java.util.Random;
import java.util.Vector;
import java.util.Collection;

/** Dirichlet distribution.
  * <p>A Dirichlet distribution assigns probability density over
  * real-valued <var>K</var>-dimensional vectors whose components
  * sum to 1.  The distribution is parametrized by a <var>K</var>-length
  * vector <b>α</b> whose components are positive real numbers greater
  * than zero.</p>
  * The probability density of the Dirichlet distribution is:
  * <blockquote>
  *   p(<b>x</b>) = 1/Z · PROD[k=0..K]
  *                       x<sub>k</sub><sup>(α<sub>k</sub>-1)</sup>
  * </blockquote>
  * <b>Connections:</b>
  * <ul><li>When <var>K</var>=2, this distribution is equivalent
  *         to the <i>Beta distribution</i>.</li>
  *     <li>The Dirichlet distribution is conjugate to the
  *         <i>Categorical distribution</i> and the
  *         <i>Multinomial distribution</i>.</li>
  * </ul>
  * @see Beta
  * @see Categorical
  * @see Multinomial */
public class Dirichlet extends SimpleDensity<Vector<Double>> {

  /** Coefficients for this Dirichlet distribution. */
  Vector<Double> alpha;
  /** Sum of the coefficients. */
  double sum;       // sum of all the alphas
  /** Number of coefficients. **/
  int dim = 0;

  /** Constructs a new uniform Dirichlet distribution of
    * dimension <i>dim</i>. */
  public Dirichlet(int dim) {
    this.dim = dim;
    this.alpha = new Vector<Double>(dim);
    this.sum = 1.0;
    for (int k=0; k<dim; k++) {
      alpha.add(1.0/(double) dim);
    }
  }
  
  /** Constructs a Dirichlet distribution of dimension <i>dim</i>,
    * with all <i>dim</i> coefficients set to <var>a</var>. */
  public Dirichlet(int dim, double a) {
    this.dim = dim;
    this.alpha = new Vector<Double>(dim);
    for (int k=0; k<dim; k++) {
      alpha.add(a);
    }
    this.sum = dim * a;
  }
  
  /** Constructs a new Dirichlet distribution with parameters
    * <i>alpha</i>. */
  public Dirichlet(Vector<Double> alpha) {
    this.alpha = alpha;
    this.dim = alpha.size();
    this.sum = 0.0;
    for (double a : alpha) {
      this.sum += a;
    }
  }
  
  /** Constructs a new Dirichlet distribution with parameters
    * <i>alpha</i>. */
  public Dirichlet(double[] alpha) {
    this.alpha = new Vector<Double>();
    this.dim = alpha.length;
    this.sum = 0.0;
    for (double a : alpha) {
      this.alpha.add(a);
      this.sum += a;
    }
  }
  
  /** Constructs a new Dirichlet distribution with parameters
    * <i>alpha</i> and a scaling constant. */
  public Dirichlet(Collection<Double> pp, double scale) {
    this.alpha = new Vector<Double>();
    this.dim = pp.size();
    this.sum = 0.0;
    for (double p : pp) {
      double ps = p*scale;
      this.alpha.add(ps);
      this.sum += ps;
    }
  }
  
  /** Constructs a new Dirichlet distribution from an existing one. */
  public Dirichlet(Dirichlet dist) {
    this.dim = dist.dim;
    this.alpha = new Vector<Double>(dist.alpha);
    this.sum = dist.sum;
  }

  /** Returns a copy of this Dirichlet distribution. */
  public Dirichlet clone() {
    return new Dirichlet(this);
  }

  /** Returns the marginal distribution of the <var>k</var>th component.
    * @param k component number (starting from zero)
    * @throws ArrayIndexOutOfBoundsException if k is not a valid
    *                                        component position */
  public Beta marginal(int k) {
    return new Beta(alpha.get(k),sum-alpha.get(k));
  }
  
  /** Returns the posterior Dirichlet distribution, given prior and data. */
  public static Dirichlet learn(Dirichlet prior, int[] data) {
    Dirichlet post = (new Dirichlet(prior));
    post.learn(data);
    return post;
  }

  /** Learn from a list of observations counts. */
  public void learn(int[] counts) {
    assert (counts.length == alpha.size());
    for (int k=0; k<counts.length; k++) {
      alpha.set(k,alpha.get(k)+counts[k]);
      sum     +=counts[k];
    }
  }

  /** Returns a string description of this distribution. */
  public String toString() {
    String vec = "[";
    if (dim > 0) {
      vec+=alpha.get(0);
      if (dim > 1) {
        for (int k=1; k < dim; k++) {
          vec += ","+alpha.get(k);
        }
      }
    }
    vec+="]";
    return "Dirichlet(A="+vec+")";
  }

  /** Returns the probability density at point <var>x</var>.
    * @return {@code Math.exp(logp(x))} */
  public double density(Vector<Double> x) {
    return Math.exp(logp(x));
  }

  /** Returns the log probability density at point <var>x</var>.
    * Same as {@code logDensity(x)}. */
  public double logDensity(Vector<Double> x) {
    assert (x.size() == alpha.size());
    double acc = 0.0;
    double lba = 0.0;
    for (int k=0; k < dim; k++) {
      acc += (alpha.get(k)-1.0)*Math.log(x.get(k));
      lba += Tools.logGamma(alpha.get(k));
    }
    return acc - lba + Tools.logGamma(sum);
  }

  
  /** Returns the marginal log likelihood, given observation counts.
    * @see #mlh(int[]) */
  public double logmlh(int[] counts) {
    assert (counts.length == alpha.size());
    double[] post = new double[dim];
    for (int k=0; k < dim; k++) {
      post[k]=alpha.get(k)+counts[k];
    }
    return Tools.logBeta(post) - Tools.logBeta(alpha);
  }

  /** Returns the marginal likelihood, given observation counts.
    * @see #logmlh(int[]) */
  public double mlh(int[] counts) {
    return Math.exp(logmlh(counts));
  }
  
  public Double[] mode() {
    Double[] mode = new Double[dim];
    for (int k=0; k<dim; k++) {
      if (alpha.get(k) >= 1.0) {
        mode[k] = (alpha.get(k)-1.0) / sum - dim;
      } else {
        return null;  // FIXME: give a sensible answer instead
      }
    }
    return mode;
  }  

  /** Returns the mean of this Dirichlet distribution. */
  public Double[] mean() {
    Double[] mean = new Double[dim];
    for (int k=0; k<dim; k++) {
      mean[k] = alpha.get(k) / sum;
    }
    return mean;
  }
 
  /** Computes the entropy of this Dirichlet distribution. */
  public double entropy() {
    double acc = 0.0;
    double dgs = Tools.digamma(sum);
    for (int k=0; k<dim; k++) {
      acc += (alpha.get(k)-1.0) * (Tools.digamma(alpha.get(k)) - dgs);
    }
    return acc - Tools.logBeta(alpha);
  }

  /** Sample from this Dirichlet distribution.
    * Works by sampling from N Gamma distributions and combining
    * the results, where N is the number of components in this
    * Dirichlet distribution.
    * @see Gamma */
  public Vector<Double> sample(Random rnd) {
    Vector<Double> sample = new Vector<Double>(dim);
    double corr = 0.0;
    for (int k=0; k<dim; k++) {
      double g = (new Gamma(alpha.get(k),1.0)).sample(rnd);
      sample.add(g);
      corr += g;
    }
    for (int k=0; k<dim; k++) {
      sample.set(k,sample.get(k) / corr);
    }
    return sample;
  }
  


}
