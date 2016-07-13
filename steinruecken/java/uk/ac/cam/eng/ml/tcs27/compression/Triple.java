/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.io.Serializable;

/** A class for immutable triples.
  * And yes, Java should be ashamed of itself for not having a sensible,
  * elegant mechanism for passing back multiple values. (How hard can it
  * be, seriously?!)
  * @see Tuple */
public class Triple<X,Y,Z> implements Serializable {
  
  private X x;
  private Y y;
  private Z z;

  /** Constructs a new triple.
    * @see #of(Object,Object,Object) */
  public Triple(X x, Y y, Z z) {
    this.x = x; this.y = y; this.z = z;
  }

  /** Returns the first component of the triple. */
  public X get0() { return x; }
  
  /** Returns the second component of the triple. */
  public Y get1() { return y; }
  
  /** Returns the third component of the triple. */
  public Z get2() { return z; }
  
  /** Returns a String representation of this triple. */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    sb.append(x.toString());
    sb.append(',');
    sb.append(y.toString());
    sb.append(',');
    sb.append(z.toString());
    sb.append(')');
    return sb.toString();
  }

  /** Static convenience method for constructing a triple.
    * This is provided simply because it's faster to type.
    * <b>Example:</b><br>
    * <blockquote><code>Triple.of(4,"cube",true)</code></blockquote>
    * as opposed to:
    * <blockquote>
    *   <code>new Triple&lt;Integer,String,Boolean&gt;(4,"cube",true)</code>
    * </blockquote> */
  public static <X,Y,Z> Triple<X,Y,Z> of(X x, Y y, Z z) {
    return new Triple<X,Y,Z>(x,y,z);
  }

}
