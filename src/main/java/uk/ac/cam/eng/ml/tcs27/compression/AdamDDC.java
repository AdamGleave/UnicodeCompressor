package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Vector;

/** The Dirichlet-discrete categorical compound distribution.
 * <p>The Dirichlet-discrete categorical compound distribution is parametrised
 * by a <var>K</var>-length vector <b>α</b> whose components are positive real numbers
 * greater than zero.</p>
 */
public class AdamDDC extends SimpleMass<Integer> implements Iterable<Integer> {
  Dirichlet dirichlet;
/*  Vector<Double> cdfUnnormalised;*/

  /** Constructs a Dirichlet-discrete categorical distribution from a
   * Dirichlet distribution, <var>prior</var>. */
  public AdamDDC(Dirichlet prior) {
    dirichlet = prior;

/*    cdfUnnormalised = new Vector<Double>(dirichlet.dim);
    double acc = 0.0;
    for (int k=0; k < dirichlet.dim; k++) {
      acc += dirichlet.alpha.get(k);
      cdfUnnormalised.set(k, acc);
    }*/
  }

  /** Copy constructor */
  public AdamDDC(AdamDDC o) {
    this.dirichlet = new Dirichlet(o.dirichlet);
  }

  /** Returns the posterior predictive Dirichlet-discrete categorical distribution,
   *  given prior and data. */
  public static AdamDDC learn(AdamDDC prior, Integer k) {
    AdamDDC post = (new AdamDDC(prior));
    post.learn(k);
    return post;
  }

  /** Learn from a list of observations counts. */
  @Override
  public void learn(Integer k) {
    dirichlet.alpha.set(k, dirichlet.alpha.get(k) + 1);
    dirichlet.sum++;
  }

  @Override
  public boolean isFinite() {
    return true;
  }

  /** Returns a string description of this distribution. */
  @Override
  public String toString() {
    return "AdamDDC(dirichlet="+dirichlet.toString()+")";
  }

  @Override
  public double mass(Integer x) {
    return dirichlet.alpha.get(x) / dirichlet.sum;
  }

  @Override
  public double logMass(Integer x) {
    return Math.log(mass(x));
  }

  public Integer mode() {
    // mode is just the value with the largest alpha
    int index = -1;
    double max = 0;

    for (int k=0; k < dirichlet.dim; k++) {
      double a = dirichlet.alpha.get(k);
      if (a > max) {
        max = a;
        index = k;
      }
    }

    return index;
  }

  /** Returns the mean of this Dirichlet-discrete categorical distribution. */
  public Double mean() {
    Double mean = 0.0;
    for (int k=0; k < dirichlet.dim; k++) {
      mean = mean + p(k) * k;
    }
    return mean;
  }

  /** Computes the entropy of this Dirichlet-discrete categorical distribution. */
  @Override
  public double entropy() {
    double acc = 0.0;
    for (int k = 0; k < dirichlet.dim; k++) {
      acc -= p(k) * logp(k);
    }
    return acc;
  }

  /** Sample from this Dirichlet-discrete categorical distribution.
   * Works by sampling from the Dirichlet distribution, and then
   * sampling from the categorical distribution with those parameters.
   * @see Dirichlet */
  @Override
  public Integer sample(Random rnd) {
    Vector<Double> p = dirichlet.sample(rnd);
    Discrete<Integer> d = Discrete.integers(p);
    return d.sample(rnd);
  }

  class RangeIterator implements Iterator<Integer> {
    private int value;
    private final int end;

    RangeIterator(int end) {
      this.value = 0;
      this.end = end;
    }

    public boolean hasNext() {
      return value < end;
    }

    public Integer next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return value++;
    }
  }

  @Override
  public Iterator<Integer> iterator() {
    return new RangeIterator(dirichlet.dim);
  }
}