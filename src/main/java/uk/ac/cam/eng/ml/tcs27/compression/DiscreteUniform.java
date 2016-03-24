/* Automated copy from build process */
/* $Id: DiscreteUniform.java,v 1.2 2013/05/15 17:31:21 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.Iterator;

/** A simple and fast uniform distribution over a discrete
  * set of elements. */
public class DiscreteUniform<X> extends SimpleMass<X> 
                                  implements Iterable<X> {

  int n = 0;
  ArrayList<X> symbols;
  double p;
  double logp;

  public DiscreteUniform(ArrayList<X> symbols) {
    this.symbols = symbols;
    this.n = symbols.size();
    this.p = 1.0 / n;
    this.logp = -Math.log(n);
  }

  public DiscreteUniform(Collection<X> syms) {
    this.symbols = new ArrayList<X>(syms);
    this.n = symbols.size();
    this.p = 1.0 / n;
    this.logp = -Math.log(n);
  }

  public DiscreteUniform(Iterable<X> syms) {
    this.symbols = new ArrayList<X>();
    this.n = 0;
    for (X x : syms) {
      this.symbols.add(x);
      this.n++;
    }
    this.p = 1.0 / n;
    this.logp = -Math.log(n);
  }

  public double mass(X x) { return p; }
  public double logMass(X x) { return logp; }

  public X sample(Random rnd) {
    int k = rnd.nextInt(n);
    return symbols.get(k);
  }

  public Iterator<X> iterator() {
    return symbols.iterator();
  }

  public void encode(X x, Encoder ec) {
    int k = symbols.indexOf(x);
    if (k > -1) {
      ec.storeRegion(k,k+1,n);
    } else {
      throw new IllegalArgumentException("element not defined: "+x);
    }
  }
  
  public X decode(Decoder dc) {
    long t = dc.getTarget(n);
    int k = (int) t;
    X x = symbols.get(k);
    dc.loadRegion(k,k+1,n);
    return x;
  }


}
