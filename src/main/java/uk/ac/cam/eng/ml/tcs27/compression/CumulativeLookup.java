/* Automated copy from build process */
/* $Id: CumulativeLookup.java,v 1.3 2012/10/12 14:07:11 chris Exp $ */

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

/** A generic discrete cumulative mass look-up table.
  * All cumulative counts are computed at instantiation time
  * and stored in a Hashtable.
  * Elements with non-zero probability mass are guaranteed to map
  * to a non-zero count increase.
  * This class is suitable for use with an arithmetic coder.
  * <p><b>Note:</b> Using a cumulative look-up table (like in this class)
  * rather than on-demand summing pays off if the same distribution is
  * used more than once.  If only one look-up is made per distribution,
  * on-demand summing is faster.</p>
  * @see Encoder
  * @see Decoder
  * @see CumulativeOnDemand */
public class CumulativeLookup<X> extends Hashtable<X,Long>
                                   implements Codable<X>, Iterable<X> {
  
  long total = 0;
  Iterable<X> order;
  Hashtable<X,X> pred = new Hashtable<X,X>();
  
  /** Creates a new CumulativeLookup table from a distribution
    * and an element ordering. */
  public CumulativeLookup(Mass<X> p, Iterable<X> syms) {
    super();
    this.order = syms;
    final long max = 0x01L << 17; // (has STRONG effect on EC-cmpr-ratio)
    long b = max;
    X before = null;
    for (X x : order) {
      if (before != null) { pred.put(x,before); }
      Double m = p.mass(x);
      if (m == null) { m = 0.0; }
      long t = (long) (m*b);
      if (t == 0) { t = 1; } // never generate a zero mass event
      total += t;
      put(x,total);
      before = x;
    }
  }

  /** Creates a new CumulativeLookup map from a Hashtable of
    * probability masses, and an element ordering. */
  public CumulativeLookup(Hashtable<X,Double> p, Iterable<X> syms) {
    super();
    this.order = syms;
    /*
    // find minimum
    double min = Double.POSITIVE_INFINITY;
    for (X x : syms) {
      Double m = p.get(x);
      if (m == null) { m = 0.0; }
      if (m < min) {
        min = m;
      }
    }
    */
    // find fraction for minimum
    //long b = (long) ((double) 1.0 / min) + 1L;
    final long max = 0x01L << 17; // (has STRONG effect on EC-cmpr-ratio)
    //if (b < 0 || b > max) {
    //  b = max;
    //}
    long b = max;
    X before = null;
    for (X x : order) {
      if (before != null) { pred.put(x,before); }
      Double m = p.get(x);
      if (m == null) { m = 0.0; }
      long t = (long) (m*b);
      if (t == 0) { t = 1; } // never generate a zero mass event
      total += t;
      put(x,total);
      before = x;
    }
  }

  public Iterator<X> iterator() {
    return order.iterator();
  }

  public void encode(X x, Encoder ec) {
    X before = pred.get(x);
    if (before != null) {
      ec.storeRegion(get(before),get(x),total);
    } else {
      ec.storeRegion(0,get(x),total);
    }
  }
    
  
  public X decode(Decoder dc) {
    long r = dc.getTarget(total);
    long before = 0;
    long here = 0;
    //for (Map.Entry<X,Long> e : this.entrySet()) {
    for (X x : order) {
      //here = e.getValue();
      here = get(x);
      // NOTE: this assumes we iterate in order of cumulative mass
      // and Hashtable / HashMap differ in this aspect, so let's be careful.
      if (here > r) {
        dc.loadRegion(before, here, total);
        //return e.getKey();
        return x;
      } else {
        before = here;
      }
    }
    return null;
  }
  
  public void encode(X x, Collection<X> omit, Encoder ec) {
    /* It shouldn't be so hard to implement this, actually. */
    throw new UnsupportedOperationException();
  }
  
  public X decode(Collection<X> omit, Decoder ec) {
    /* It shouldn't be so hard to implement this, actually. */
    throw new UnsupportedOperationException();
  }


  public static <X> void encode(Hashtable<X,Double> p, X x, Encoder ec) {
    CumulativeLookup<X> cm = new CumulativeLookup<X>(p, p.keySet());
    cm.encode(x,ec);
  }
  
  public static <X> X decode(Hashtable<X,Double> p, Decoder dc) {
    CumulativeLookup<X> cm = new CumulativeLookup<X>(p, p.keySet());
    return cm.decode(dc);
  }

}



