/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Random;

/** A move-to-front encoder. */
public class MTF<X> extends SimpleMass<X> implements AdaptiveCode<X> {
  
  protected Distribution<Integer> dist;
  protected Iterable<X> alphabet;
  protected LinkedList<X> list;

  /** Constructs a new move-to-front encoder for
    * given alphabet and distribution. */
  public MTF(Distribution<Integer> d, Iterable<X> alphabet) {
    this.dist = d;
    this.alphabet = alphabet;
    this.list = new LinkedList<X>();
    for (X x : alphabet) { list.add(x); }
  }

  public String toString() {
    return "MTF(<set>,"+dist.toString()+")";
  }
  
  /** Return the current position of the given symbol. */
  public int index(X x) {
    return list.indexOf(x);
  }

  public double mass(X x) {
    int k = list.indexOf(x);
    return dist.mass(k);
  }
  
  public double logMass(X x) {
    int k = list.indexOf(x);
    return dist.logMass(k);
  }

  /** Move the given symbol to the front. */
  public void learn(X x) {
    // move to front
    int k = list.indexOf(x);
    dist.learn(k);
    list.remove(x);
    list.offerFirst(x);
  }

  public void encode(X x, Encoder ec) {
    int k = list.indexOf(x);
    dist.encode(k,ec);
  }
  
  public X decode(Decoder dc) {
    int k = dist.decode(dc);
    return list.get(k);
  }
  
  public X sample(Random rnd) {
    int k = dist.sample(rnd);
    return list.get(k);
  }
  
  public Distribution<X> getPredictiveDistribution() {
    Hashtable<X,Double> table = new Hashtable<X,Double>();
    for (int k=0; k<list.size(); k++) {
      table.put(list.get(k), dist.mass(k));
    }
    return new DiscreteLookup<X>(table);
  }

}
