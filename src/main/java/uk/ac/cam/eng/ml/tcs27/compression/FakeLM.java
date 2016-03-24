/* Automated copy from build process */
/* $Id: FakeLM.java,v 1.1 2012/06/01 01:36:55 chris Exp $ */

import java.util.Random;
import java.util.Collection;

/** A naïve, fake language model.
  * FakeLM generates sequences of characters made from a concatenated
  * sequence of words, drawn from a Pitman-Yor CRP over random words. */
public class FakeLM extends SimpleMass<Character> {

  /** Generator of the vocabulary (base distribution over words). */
  public Distribution<String> words = null;

  /** Generator of the word distribution (PY process). */
  public CRPV<String> wdist = null;

  /** Current word. */
  private String word = null;
  /** Current character position. */
  private int cpos = -1;


  /** Creates a new fake language model.
    * @param sd distribution over words */
  public FakeLM(Distribution<String> sd) {
    words = sd;
    wdist = new CRPV<String>(4,1, 1,2, words);
  }
  
  /** Creates a new fake language model. */
  public FakeLM() {
    words = new NaiveStringDist();
    wdist = new CRPV<String>(4,1, 1,2, words);
  }

  public Character sample(Random rnd) {
    Character res = null;
    if (word == null) {
      word = wdist.sample(rnd);
      cpos = 0;
    }
    if (cpos < word.length()) {
      res = word.charAt(cpos);
      cpos++;
    } else {
      word = null;
      res = ' '; // "space" character
    }
    return res;
  }

  public void sample(Random rnd, int n, Collection<Character> col) {
    for (int k=0; k<n; k++) {
      col.add(sample(rnd));
    }
  }

  public double logMass(Character c) {
    throw new UnsupportedOperationException();
  }
  public double mass(Character c) {
    throw new UnsupportedOperationException();
  }

}
