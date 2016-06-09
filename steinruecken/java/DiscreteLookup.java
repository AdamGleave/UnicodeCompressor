/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Iterator;

/** A generic discrete mass function, using a lookup table.
  * Implemented using a Hashtable.
  * @see Discrete
  * @see DiscreteInteger
  * @see CumulativeLookup */
public class DiscreteLookup<X> extends SimpleMass<X> implements Iterable<X> {

  Hashtable<X,Double> table;

  public double mass(X x) {
    return table.get(x);
  }
 
  public double logMass(X x) {
    return Math.log(table.get(x));
  }

  public X sample(Random rnd) {
    return Samplers.sample(rnd, table);
  }

  public String toString() {
    return "DiscreteLookup["+table.size()+"]";
  }

  public Iterator<X> iterator() {
    return table.keySet().iterator();
  }

  /** Force-normalizes this probability distribution. */
  public synchronized void normalize() {
    double sum = 0.0;
    for (Double d : table.values()) {
      sum += d;
    }
    Hashtable<X,Double> newtab = new Hashtable<X,Double>();
    for (Map.Entry<X,Double> e : table.entrySet()) {
      newtab.put(e.getKey(), e.getValue() / sum);
    }
    table = newtab;
  }
  
  /** Force-inverts this probability distribution, producing
    * its complement.
    * The complement P' of a distribution P puts mass
    * P'(<var>x</var>) = 1/Z · (1–P(<var>x</var>))
    * on every element <var>x</var>. */
  public synchronized void invert() {
    double sum = 0.0;
    for (Double d : table.values()) {
      sum += (1.0 - d);
    }
    Hashtable<X,Double> newtab = new Hashtable<X,Double>();
    for (Map.Entry<X,Double> e : table.entrySet()) {
      newtab.put(e.getKey(), (1.0 - e.getValue()) / sum);
    }
    table = newtab;
  }

  /** Constructs an empty DiscreteLookup. */
  public DiscreteLookup() {
    this.table = new Hashtable<X,Double>();
  }

  /** Constructs a new DiscreteLookup from an existing mass function
    * and an iterable collection of elements.
    * @param mf probability mass function
    * @param col iterable collection of elements */
  public DiscreteLookup(Mass<X> mf, Iterable<X> col) {
    this.table = new Hashtable<X,Double>();
    for (X x : col) { this.table.put(x,mf.mass(x)); }
  }
  
  /** Constructs a DiscreteLookup around an existing Hashtable.
    * The table is referenced by pointer, not copied.
    * @param table probability mass table */
  public DiscreteLookup(Hashtable<X,Double> table) {
    this.table = table;
  }
  
  /** Constructs a new DiscreteLookup from an existing map.
    * @param map probability mass function */
  public DiscreteLookup(Map<X,Double> map) {
    this.table = new Hashtable<X,Double>();
    this.table.putAll(map);
  }
  
  /** Constructs a new DiscreteLookup from a map of counts.
    * The counts in the supplied histogram are normalized
    * to form a valid probability distribution.
    * @param counts counts for each element
    * @param total sum of all counts */
  public DiscreteLookup(Map<X,Integer> counts, int total) {
    this.table = new Hashtable<X,Double>();
    double t = (double) total;
    for (Map.Entry<X,Integer> e : counts.entrySet()) {
      this.table.put(e.getKey(), (double) e.getValue() / t);
    }
  }
  
  /** Constructs a new DiscreteLookup from a histogram.
    * The counts in the supplied histogram are normalized
    * to form a valid probability distribution. */
  public static <X> DiscreteLookup<X> fromCounts(Map<X,Integer> hist) {
    DiscreteLookup<X> dl = new DiscreteLookup<X>();
    int total = 0;
    for (Map.Entry<X,Integer> e : hist.entrySet()) {
      total += e.getValue();
    }
    return new DiscreteLookup<X>(hist,total);
  }
  
  /** Constructs a uniform DiscreteLookup from a finite iterable
    * collection of elements.
    * @param col iterable collection of elements */
  public DiscreteLookup(Iterable<X> col) {
    int count = 0;
    for (X x : col) {
      count++;
    }
    this.table = new Hashtable<X,Double>();
    double p = 1.0 / count;
    for (X x : col) {
      this.table.put(x,p);
    }
  }
  
  @Override
  public void encode(X x, Encoder ec) {
    long budget = ec.getRange();
    Map<X,Long> mass = Coding.getDiscreteMass(this,this,budget);
    Coding.encode(x, mass, this, ec);
  }

  @Override
  public void encode(X x, Collection<X> omit, Encoder ec) {
    throw new UnsupportedOperationException();
  }

  @Override
  public X decode(Decoder dc) {
    long budget = dc.getRange();
    Map<X,Long> mass = Coding.getDiscreteMass(this,this,budget);
    return Coding.decode(mass, this, dc);
  }
  
  @Override
  public X decode(Collection<X> omit, Decoder dc) {
    throw new UnsupportedOperationException();
  }

}
