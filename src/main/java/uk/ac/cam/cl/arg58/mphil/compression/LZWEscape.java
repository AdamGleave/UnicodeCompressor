/* Automated copy from build process */
/* $Id: LZW.java,v 1.14 2015/08/11 11:28:16 chris Exp $ */
package uk.ac.cam.cl.arg58.mphil.compression;

import uk.ac.cam.eng.ml.tcs27.compression.*;

import java.util.*;

/** The LZW compression algorithm.
  * This implementation is a variant of LZW which uses arithmetic
  * coding for the storage of dictionary indices.
  * <p>The class has built-in (but optional) support for an EOF
  * symbol, which is encoded like an ordinary symbol, but also
  * flushes the remaining input symbol queue.  If an EOF symbol
  * is not desired, a manual call to the <code>flush(..)</code>
  * method can be made to complete encoding.</p>
  * <p><b>Warning:</b> This has encoding and decoding support, but
  * cannot directly compute the probability mass of elements in X,
  * nor can it sample from X.</p>
  * <p><b>Warning:</b> The <code>learn(..)</code> method is not
  * doing any learning; the learning is different for encoding
  * and decoding and implemented directly in <code>encode(..)</code>
  * and <code>decode(..)</code>.</p>
  * <dl><dt><b>References:</b></dt>
  * <dd><ul><li alt="[1]"><a name="welch1984a">T. A. Welch.&nbsp;
  * <i>A Technique for High-Performance Data Compression,</i>
  * 1984-06. In <i>Computer</i>, Vol. 17, No. 6, pp. 8-16. IEEE.</a></li>
  * </ul></dd></dl> */
public class LZWEscape<X> extends SimpleMass<X>
                          implements Codable<X>, AdaptiveCode<X> {

  /** End-of-file marker. */
  X EOF = null;

  /** Alphabet size. */
  int size = 0;
  /** Dictionary size. */
  int words = 0;
  /** Index distribution. */
  UniformInteger u = null;

  /** Read buffer. */
  Vector<X> rbuf = new Vector<X>();
  int rhash = 0;
  /** Write buffer. */
  Vector<X> wbuf = new Vector<X>();
  int whash = 0;


  HashMap<Vector<X>,Integer> rdict = new HashMap<Vector<X>,Integer>();
  HashMap<Integer,Vector<X>> wdict = new HashMap<Integer,Vector<X>>();

  Set<X> alphabet = new HashSet<X>();
  Distribution<X> base;

  /** Last used dictionary index while decoding. */
  private int last = -1;


  /** Constructs a new LZW process with given base distribution. */
  public LZWEscape(Distribution<X> base) {
    rdict.put(new Vector<X>(), 0);
    wdict.put(0, new Vector<X>());

    u = new UniformInteger(0,0);
    size = 1;   // alphabet size
    words = 1;  // (initial) dictionary size
    this.base = base;
  }

  /** Constructs a new LZW process with given alphabet and EOF marker. */
  public LZWEscape(Distribution<X> base, X eof) {
    this(base);
    EOF = eof;
  }


  /** Inserts a word into rdict, the read dictionary. */
  protected void insertR(Vector<X> newentry) {
    if (rdict.containsKey(newentry)) {
      System.err.println("Warning: ALREADY IN DICT: "+newentry);
    }
    rdict.put(newentry, words);
    words++;
  }
  

  /** Returns an identification of this LZW compressor. */
  public String toString() {
    return "LZW(|alphabet|="+size+", |dict|="+words+")";
  }

  private void printRDict() {
    for (Map.Entry<Vector<X>,Integer> w : rdict.entrySet()) {
      if (w.getValue() >= size) {
        System.err.println(w.getValue()+": "+w.getKey());
      }
    }
  }
  private void printWDict() {
    for (int k=size; k<words; k++) {
      System.err.println(k+": "+wdict.get(k));
    }
  }


  public void flush(Encoder ec) {
    // encode what's left in the read buffer
    if (rbuf.size() > 0) {
      int k = rdict.get(rbuf);
      u.encode(k,ec);  // encode it
      rbuf.clear();
    }
  }

  public void encode(X x, Encoder ec) {
    /* Case 1: buffer is empty: simply append x
     * Case 2: buffer+x is in dictionary: append x.
     * Case 3: buffer+x is NOT in dictionary:
     *            output index(buffer),
     *            add "buffer+x" to dictionary,
     *            and set buffer to x.
     */

    // invariant: rbuf is contained in current dictionary
    Vector<X> tmpbuf = new Vector<X>(rbuf);
    tmpbuf.add(x);
    // check if new rbuf is still contained in dictionary
    if (rdict.containsKey(tmpbuf)) {
      rbuf.add(x);
    } else {
      /* current buffer is no longer in the dictionary:
       * so let's write the index of the currently longest
       * dictionary word, and store a new word. */
      int k = rdict.get(rbuf);
      u.encode(k, ec); // dictionary index

      insertR(tmpbuf);
      u.expand(1);
      rbuf.clear();

      if (k == 0) { // escape symbol
        // rbuf = []; tmpbuf = [x] and is not in the dictionary
        base.encode(x, alphabet, ec); // encode in base
        alphabet.add(x);
      } else {
        // have encoded rbuf, but not x; try again
        encode(x, ec);
      }
    }

    if (EOF.equals(x)) {
      // flush symbols still in the buffer
      learn(x);
      flush(ec);
    }
  }

  public X decode(Decoder dc) {
    X x;
    if (wbuf.size() == 0) {
      int k = u.decode(dc);         // decode dictionary index

      if (k == 0) { // escape symbol
        x = base.decode(alphabet, dc);
        alphabet.add(x);
        wbuf = new Vector<X>();
        wbuf.add(x);
      } else {
        wbuf = new Vector<X>(wdict.get(k));  // copy word from dictionary
        x = wbuf.get(0); // fetch first symbol
      }

      // complete last dictionary entry
      if (last != -1) {
        Vector<X> obuf = new Vector<X>(wdict.get(last));
        obuf.add(x);               // add missing last symbol: x
        wdict.put(last,obuf);      // write completed word back to dict
      }
      if (k == last) { // we decoded the last dictionary entry (incomplete), fix it up
        wbuf.add(x);
      }

      // add (possibly partial) new word to dictionary
      Vector<X> nbuf = new Vector<X>(wbuf);
      wdict.put(words,nbuf);      // write incomplete word to dict
      last = (k == 0) ? -1 : words; // if k == 0, the word is complete not partial
      words++;
      u.expand(1);
    } else {
      x = wbuf.get(0);
    }

    wbuf.remove(0);
    return x;
  }

  public void encode(X x, Collection<X> col, Encoder ec) {
    throw new UnsupportedOperationException();
  }
  public X decode(Collection<X> col, Decoder dc) {
    throw new UnsupportedOperationException();
  }
  
  public Distribution<X> getPredictiveDistribution() {
    throw new UnsupportedOperationException();
  }

  public double logMass(X x) { throw new UnsupportedOperationException(); }
  
  public double mass(X x) { throw new UnsupportedOperationException("Not implemented: sample."); }
  
  public void learn(X x) {
    // no-op
  }

  public X sample(Random rnd) { throw new UnsupportedOperationException(); }
}
