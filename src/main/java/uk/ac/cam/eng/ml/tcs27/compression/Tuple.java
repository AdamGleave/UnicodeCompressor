package uk.ac.cam.eng.ml.tcs27.compression;
/* Automated copy from build process */
/* $Id: Tuple.java,v 1.3 2015/08/11 11:28:16 chris Exp $ */

import java.io.Serializable;

/** A class for immutable tuples.
  * And yes, Java should be ashamed of itself for not having a sensible,
  * elegant mechanism for passing back multiple values. (How hard can it
  * be, seriously?!) */
public class Tuple<X,Y> implements Serializable {
  
  private X x;
  private Y y;

  /** Constructs a new tuple.
    * @see #of(Object,Object) */
  public Tuple(X x, Y y) {
    this.x = x; this.y = y;
  }

  /** Returns the first component of the tuple. */
  public X get0() { return x; }
  
  /** Returns the second component of the tuple. */
  public Y get1() { return y; }
  
  /** Returns a String representation of this tuple. */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    sb.append(x.toString());
    sb.append(',');
    sb.append(y.toString());
    sb.append(')');
    return sb.toString();
  }

  /** Static convenience method for constructing a tuple.
    * This is provided simply because it's faster to type.
    * <b>Example:</b><br>
    * <blockquote><code>Tuple.of(27,"cube")</code></blockquote>
    * as opposed to:
    * <blockquote>
    *   <code>new Tuple&lt;Integer,String&gt;(27,"cube")</code>
    * </blockquote> */
  public static <X,Y> Tuple<X,Y> of(X x, Y y) {
    return new Tuple<X,Y>(x,y);
  }

}
