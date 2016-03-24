/* Automated copy from build process */
/* $Id: MapDist.java,v 1.9 2015/08/11 11:28:16 chris Exp $ */

import java.util.Random;
import java.util.Iterator;
import java.util.Collection;
import java.util.AbstractCollection;
import java.io.Serializable;

/** Maps a distribution onto a different domain, given an elementwise
  * bidirectional mapping.
  * <p>Subclasses must implement <code>raise(<var>x</var>)</code> and
  * <code>lower(<var>y</var>)</code>, such that
  *     <code><var>y</var> = raise(lower(<var>y</var>))</code>
  * and <code><var>x</var> = lower(raise(<var>x</var>))</code>
  * for all <var>x</var> and <var>y</var>.<br>
  * The rest is automatic.</p>
  * <dl><dt><b>Example code:</b></dt>
  * <dd><pre>
  * new MapDist&lt;E,Vector&lt;E&gt;&gt;(Discrete.integers(0,42)) {
  *   public Vector&lt;E&gt; raise(E e) {
  *     Vector&lt;E&gt; v = new Vector&lt;E&gt;(1); v.add(e); return v;
  *   }
  *   public E lower(Vector&lt;E&gt; v) {V
  *     return v.get(0);
  *   }
  * }
  * </pre></dd></dl> */
public abstract class MapDist<X,Y> extends Distribution<Y>
                                   implements Serializable {

  Distribution<X> base = null;

  /** Constructs a new MapDist from base distribution <var>xdist</var>. */
  public MapDist(Distribution<X> xdist) {
    this.base = xdist;
  }

  /** Maps an element of X to the corresponding element of Y. */
  public abstract Y raise(X x);

  /** Maps an element of Y to the corresponding element of X. */
  public abstract X lower(Y y);
  
  public String toString() {
    return "["+base.toString()+", mapped]"; 
  }

  /* some useful tools */
  
  /** Transforms an iterator over X to an iterator over Y. */
  public Iterator<Y> raise(final Iterator<X> it) {
    return new Iterator<Y>() {
      public boolean hasNext() { return it.hasNext(); }
      public Y next() { return raise(it.next()); }
      public void remove() { it.remove(); }
    };
  }
  
  /** Transforms an iterator over Y to an iterator over X. */
  public Iterator<X> lower(final Iterator<Y> it) {
    return new Iterator<X>() {
      public boolean hasNext() { return it.hasNext(); }
      public X next() { return lower(it.next()); }
      public void remove() { it.remove(); }
    };
  }

  /** Transforms a collection of X to a collection of Y. */
  public Collection<Y> raise(final Collection<X> col) {
    return new AbstractCollection<Y>() {
      public Iterator<Y> iterator() { return raise(col.iterator()); }
      public int size() { return col.size(); }
    };
  }
  
  /** Transforms a collection of Y to a collection of X. */
  public Collection<X> lower(final Collection<Y> col) {
    return new AbstractCollection<X>() {
      public Iterator<X> iterator() { return lower(col.iterator()); }
      public int size() { return col.size(); }
    };
  }

  /* methods of the Distribution interface */

  public Y sample(Random rnd) {
    return raise(base.sample(rnd));
  }
  public double mass(Y y) {
    return base.mass(lower(y));
  }
  public double logMass(Y y) {
    return base.logMass(lower(y));
  }
  public double density(Y y) {
    return base.density(lower(y));
  }
  public double logDensity(Y y) {
    return base.logDensity(lower(y));
  }
  public double p(Y y) {
    return base.p(lower(y));
  }
  public double logp(Y y) {
    return base.logp(lower(y));
  }
  public void encode(Y y, Encoder ec) {
    base.encode(lower(y),ec);
  }
  public Y decode(Decoder dc) {
    return raise(base.decode(dc));
  }
  public void encode(Y y, Collection<Y> omit, Encoder ec) {
    base.encode(lower(y), lower(omit), ec);
  }
  public Y decode(Collection<Y> omit, Decoder dc) {
    return raise(base.decode(lower(omit), dc));
  }
  public void learn(Y y) {
    base.learn(lower(y));
  }

  public double totalMass(final Iterable<Y> col) {
    return base.totalMass(new Iterable<X>() {
      public Iterator<X> iterator() {
        final Iterator<Y> it = col.iterator();
        return new Iterator<X>() {
          public boolean hasNext() {
            return it.hasNext();
          }
          public X next() {
            return lower(it.next());
          }
          public void remove() {
            it.remove();
          }
        };
      }
    });
  }

}
