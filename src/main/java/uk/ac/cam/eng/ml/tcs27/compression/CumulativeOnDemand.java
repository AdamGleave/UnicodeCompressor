/* Automated copy from build process */
/* $Id: CumulativeOnDemand.java,v 1.3 2013/10/12 18:20:07 chris Exp $ */
/* FIXME: this class uses outdated methods, not taking advantage of
 * Encoder.getRange()! Compression effectiveness might thus be lower. */

import java.util.Collection;
import java.util.Hashtable;

/** A sum-on-demand discrete cumulative mass functor.
  * Cumulative counts are computed when needed, and are not cached,
  * except for the total count.
  * Elements with non-zero probability mass always map to non-zero
  * count increases.
  * This class is suitable for use with an arithmetic coder.
  * @see Encoder
  * @see Decoder
  * @see CumulativeLookup */
public class CumulativeOnDemand<X> implements Codable<X> {
  
  final long max = 0x01L << 17; // (has STRONG effect on EC-cmpr-ratio)
  Long total = null;
  Mass<X> mf;
  Iterable<X> order;

  /** Creates a new CumulativeOnDemand functor from a mass function
    * and an element ordering. */
  public CumulativeOnDemand(Mass<X> p, Iterable<X> syms) {
    this.mf = p;
    this.order = syms;
    this.total = null;
    throw new RuntimeException("deprecated!");
  }
  

  public long countUpTo(X x) {
    long count = 0L;
    for (X y : order) {
      if (y.equals(x)) { return count; }
      double m = mf.mass(y);
      long t = (long) (m*max);
      if (t == 0) { t = 1; } // never generate a zero mass event
      count += t;
    }
    throw new IllegalArgumentException("Unlisted element: "+x);
  }
  
  public void getTotal() {
    total = 0L;
    for (X x : order) {
      Double m = mf.mass(x);
      long t = (long) (m*max);
      if (t == 0) { t = 1; } // never generate a zero mass event
      total += t;
    }
  }

  public void encode(X x, Encoder ec) {
    long lo = 0L;
    long mx = 0L;
    if (total == null) {
      long count = 0L;
      for (X y : order) {
        double m = mf.mass(y);
        long t = (long) (m*max);
        if (t == 0) { t = 1; }  // never generate a zero mass event
        if (y.equals(x)) {
          lo = count;
          mx = t;
        }
        count += t;
      }
      total = count;
      ec.storeRegion(lo, lo+mx, total);
    } else {
      long before = countUpTo(x);
      double m = mf.mass(x);
      long t = (long) (m*max);
      if (t == 0) { t = 1; }  // never generate a zero mass event
      ec.storeRegion(before,before+t,total);
    }
  }
    
  
  public X decode(Decoder dc) {
    if (total == null) { getTotal(); }
    long r = dc.getTarget(total);
    long before = 0;
    long here = 0;
    for (X x : order) {
      double m = mf.mass(x);
      long t = (long) (m*max);
      if (t == 0) { t = 1; }
      here += t;
      if (here > r) {
        dc.loadRegion(before, here, total);
        return x;
      } else {
        before = here;
      }
    }
    throw new java.util.NoSuchElementException();
  }
  
  public void encode(X x, Collection<X> omit, Encoder ec) {
    /* It shouldn't be so hard to implement this, actually. */
    throw new UnsupportedOperationException();
  }
  
  public X decode(Collection<X> omit, Decoder ec) {
    /* It shouldn't be so hard to implement this, actually. */
    throw new UnsupportedOperationException();
  }


}



