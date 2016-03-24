/* Automated copy from build process */
/* $Id: UniformChar.java,v 1.12 2013/04/08 15:49:11 chris Exp $ */

import java.util.Random;
import java.util.Vector;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;

/** A uniform probability distribution over any contiguous
  * range of Java characters.
  * @see Discrete */
public class UniformChar extends SimpleMass<Character>
                                 implements Codable<Character>,
                                            Iterable<Character> {

  /** Minimum Unicode code point (inclusive). */
  public int minpoint = 0x0000;
  /** Maximum Unicode code point (inclusive). */
  public int maxpoint = 0xffff;

  /** Number of chars with non-zero probability mass. */
  protected int points = 0x10000;

  /** Creates a uniform distribution over the full range of
    * 16-bit Java characters. */
  public UniformChar() {
  }

  /** Creates a uniform distribution over characters in
    * range <var>a</var> to <var>b</var> (inclusive). */
  public UniformChar(int a, int b) {
    minpoint = a;
    maxpoint = b;
    points = b-a+1;
  }
  
  /** Creates a uniform distribution over characters in
    * range <var>a</var> to <var>b</var> (inclusive). */
  public UniformChar(char a, char b) {
    minpoint = (int) a;
    maxpoint = (int) b;
    points = maxpoint-minpoint+1;
  }

  /** Returns a uniformly sampled character. */
  public Character sample(Random rnd) {
    return (char) (rnd.nextInt(points)+minpoint);
  }

  /** Returns the log probability mass of character <var>c</var>. */
  public double logMass(Character c) {
    return (c >= minpoint && c <= maxpoint) ? -Math.log(points)
                                            : Double.NEGATIVE_INFINITY;
  }
  
  /** Returns the probability mass of character <var>c</var>. */
  public double mass(Character c) {
    return (c >= minpoint && c <= maxpoint) ? 1.0/points : 0.0;
  }
  
  /** Returns if this distribution is defined over a finite set of
    * elements.  For the UniformChar class, this is always true.
    * @return true */
  public boolean isFinite() {
    return true;
  }

  /** Returns a String description of this distribution. */
  public String toString() {
    return "UniformChar(0x"+Integer.toHexString(minpoint)
                    +"..0x"+Integer.toHexString(maxpoint)+")";
  }

  public long discreteMass(Character c) {
    return (c >= minpoint && c <= maxpoint) ? 1 : 0;
  }
  
  public long discreteTotalMass() {
    return points;
  }


  /* ------------------------------------------------------------- */

  public void encode(Character c, Encoder ec) {
    long k = (c - minpoint);
    ec.storeRegion(k,k+1,points);
  }
  
  public void encode(Character c, Collection<Character> omit, Encoder ec) {
    long rtotal  = 0;
    long rbefore = 0;
    for (char o : omit) {
      if (o >= minpoint && o <= maxpoint) {
        // record "to be omitted" characters which are in range
        rtotal++;
        if (o < c) {
          // and count how many occur before c
          rbefore++;
        } else
        if (o == c) {
          // obviously c had better not be omitted
          throw new IllegalArgumentException("attempt to encode an excluded"
                                            +" character");
        }
      }
    }
    long k = (c - minpoint - rbefore);
    ec.storeRegion(k,k+1,points-rtotal);
  }
  
  public Character decode(Decoder dc) {
    long i = dc.getTarget(points);
    char c = (char) (minpoint + i);
    dc.loadRegion(i,i+1,points);
    return c;
  }
  
  public Character decode(Collection<Character> omit, Decoder dc) {
    long rtotal  = 0;
    long rbefore = 0;
    for (char o : omit) {
      if (o >= minpoint && o <= maxpoint) {
        // record "to be omitted" characters which are in range
        rtotal++;
      }
    }
    if (rtotal == 0) {
      // fall back on cheap and simple method
      return decode(dc);
    } else {
      long i = dc.getTarget(points-rtotal);
      char c = (char) minpoint;
      long k = 0;
      // this could probably be done more efficiently...
      while (omit.contains(c)) {
        c++;
      }
      while (k < i) {
        k++; c++;
        while (omit.contains(c)) {
          c++;
        }
      }
      dc.loadRegion(i,i+1,points-rtotal);
      return c;
    }
  }
  

  /* ------------------------------------------------------------- */

  /** Returns an iterator over the defined range of characters. */
  public Iterator<Character> iterator() {
    return new Iterator<Character>() {
      int k = minpoint;
      public boolean hasNext() {
        return (k <= maxpoint);
      }
      public Character next() {
        if (k <= maxpoint) {
          char c = (char) k;
          k++;
          return c;
        } else {
          throw new NoSuchElementException();
        }
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /* ------------------------------------------------------------- */


  /** Returns a uniform distribution over 7-bit ASCII characters. */
  public static UniformChar ascii() {
    return new UniformChar(0x0000, 0x007f);
  }
  
  /** Returns a uniform distribution over decimal ASCII digits 0-9. */
  public static UniformChar asciiDigits() {
    return new UniformChar('0', '9');
  }
  
  /** Returns a uniform distribution over printable ASCII
    * characters. Control characters 0x00 - 0x19 are omitted,
    * as is 0x7f. */
  public static UniformChar asciiPrintable() {
    return new UniformChar(0x0020, 0x007e);
  }
  
  /** Returns a uniform distribution over the first 256 characters. */
  public static UniformChar first256() {
    return new UniformChar(0x0000, 0x00ff);
  }
  
  /** Returns a uniform distribution over characters from
    * range 0x0000 to 0x2FFF. */
  public static UniformChar first3000() {
    return new UniformChar(0x0000, 0x2fff);
  }
  
  /** Returns a uniform distribution over die-face characters.
    * 0x2680..0x2685 = {⚀, ⚁, ⚂, ⚃, ⚄, ⚅}. */
  public static UniformChar diefaces() {
    return new UniformChar(0x2680, 0x2685);
  }
  

  /* ------------------------------------------------------------- */

  /** Exports this UniformChar distribution as {@code Discrete} distribution.
    * The implementation of Discrete is more generic and flexible, but
    * it requires storing the entire range of characters in memory.
    * Sampling may be less efficient, but only by a small constant. */
  public Discrete<Character> toDiscrete() {
    Vector<Character> v = new Vector<Character>(points);
    for (int k=minpoint; k<=maxpoint; k++) {
      v.add((char) k);
    }
    return new Discrete<Character>(v);
  }

}
