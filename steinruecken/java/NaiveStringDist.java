/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.Random;
import java.util.Vector;
import java.util.Collection;

/** Simple distribution over Strings. This distribution assumes
  * that string length is independent of string content, and that
  * each character of the string is drawn independently.
  * The distribution is parametrized by a distribution on
  * string length, and a distribution over characters. */
public class NaiveStringDist extends SimpleMass<String> 
                                         implements Codable<String> {

  /** String length distribution. */
  Distribution<Integer> ld = null;

  /** Codable instance for the string length distribution. */
  Codable<Integer> ldcode = null;

  /** Character distribution. */
  Distribution<Character> cd = null;
  
  /** Codable instance for the character distribution. */
  Codable<Character> cdcode = null;

  /** Constructs a new naïve string distribution.
    * @param length distribution over string lengths
    * @param base distribution over component characters */
  @SuppressWarnings("unchecked")
  public NaiveStringDist(Distribution<Integer> length,
                         Distribution<Character> base) {
    this.ld = length;
    this.cd = base;
    if (ld instanceof Codable) {
      ldcode = (Codable<Integer>) ld;
    }
    if (cd instanceof Codable) {
      cdcode = (Codable<Character>) cd;
    }
  }

  /** Constructs a naïve string distribution with default parameters. */
  public NaiveStringDist() {
    this(new Binomial(8,0.9), new UniformChar('a','z'));
  }

  public double logMass(String s) {
    int l = s.length();
    double logp = ld.logp(l);
    for (int k=0; k<l; k++) {
      logp += cd.logMass(s.charAt(k));
    }
    return logp;
  }
  
  public double mass(String s) {
    return Math.exp(logMass(s));
  }

  public String sample(Random rnd) {
    int l = ld.sample(rnd);
    StringBuilder sb = new StringBuilder();
    for (int k=0; k<l; k++) {
      sb.append(cd.sample(rnd));
    }
    return sb.toString();
  }

  public String toString() {
    return cd.toString()+"^n; n ∼ "+ld.toString();
  }

  public void learn(String s) {
    int l = s.length();
    ld.learn(l);
    for (int k=0; k<l; k++) {
      cd.learn(s.charAt(k));
    }
  }

  /** Returns if this distribution is defined over a finite number
    * of Strings.  This is true if and only if the length distribution
    * is finite (character distributions cannot be infinite as there
    * are only finitely many characters). */
  public boolean isFinite() {
    return ld.isFinite();
  }

  public void encode(String s, Encoder ec) {
    if (ldcode != null && cdcode != null) {
      int n = s.length();
      // add path from length component
      ldcode.encode(n,ec);
      // add paths from characters
      for (int k=0; k<n; k++) {
        cdcode.encode(s.charAt(k),ec);
      }
    } else {
      throw new UnsupportedOperationException("component distributions"
                                            +" don't implement Codable");
    }
  }
  
  public void encode(String s, Collection<String> without, Encoder ec) {
    if (without.contains(s)) {
      throw new IllegalArgumentException("attempt to encode excluded String");
    }
    // but really we're ignoring excluded strings
    // it just doesn't seem worth it...
    encode(s,ec);
  }

  public String decode(Decoder dc) {
    if (ldcode != null && cdcode != null) {
      Integer n = ldcode.decode(dc);
      //System.err.print("<"+n+">");
      StringBuilder sb = new StringBuilder();
      for (int k=0; k<n; k++) {
        Character x = cdcode.decode(dc);
        //System.err.print("<"+x+","+(int) ((char) x)+">");
        sb.append(x);
      }
      return sb.toString();
    } else {
      throw new UnsupportedOperationException("component distributions don't implement Codable");
    }
  }
  
  public String decode(Collection<String> without, Decoder dc) {
    // we're actually just ignoring excluded strings...
    return decode(dc);
  }

}
