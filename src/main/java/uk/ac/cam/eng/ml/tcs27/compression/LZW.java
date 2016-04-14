/* Automated copy from build process */
/* $Id: LZW.java,v 1.14 2015/08/11 11:28:16 chris Exp $ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collection;
import java.util.Vector;
import java.util.Random;
import java.io.InputStream;

/** The LZW compression algorithm.
  * This implementation is a variant of LZW which uses arithmetic
  * coding for the storage of dictionary indices.
  * <p>The class has built-in (but optional) support for an EOF
  * symbol, which is encoded like an ordinary symbol, but also
  * flushes the remaining input symbol queue.  If an EOF symbol
  * is not desired, a manual call to the <code>flush(..)</code>
  * method can be made to complete encoding.</p>
  * <dl><dt><b>References:</b></dt>
  * <dd><ul><li alt="[1]"><a name="welch1984a">T. A. Welch.&nbsp;
  * <i>A Technique for High-Performance Data Compression,</i>
  * 1984-06. In <i>Computer</i>, Vol. 17, No. 6, pp. 8-16. IEEE.</a></li>
  * </ul></dd></dl> */
public class LZW<X> extends SimpleMass<X>
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

  /** Symbol alphabet. */
  Iterable<X> alphabet;

  HashMap<Vector<X>,Integer> rdict = new HashMap<Vector<X>,Integer>();
  HashMap<Integer,Vector<X>> wdict = new HashMap<Integer,Vector<X>>();
  
  //HashMap<List<X>,Integer> prefixcount = new HashMap<List<X>,Integer>();
  HashMap<List<X>,Integer> prefixcount = null;

  /** Last used dictionary index while decoding. */
  private int last = -1;

  private boolean decoding = false;


  /** Constructs a new LZW process with given alphabet. */
  public LZW(Iterable<X> alphabet) {
    int n = 0;
    for (X x : alphabet) {
      Vector<X> v = new Vector<X>(1);
      v.add(x);
      rdict.put(v,n);
      wdict.put(n,v);
      n++;
    }
    u = new UniformInteger(0,n-1);
    this.size = n;   // alphabet size
    this.words = n;  // (initial) dictionary size
    this.alphabet = alphabet;
    //rebuildPrefixCounts(); // wait until mass() is used
  }
  
  /** Constructs a new LZW process with given alphabet and EOF marker. */
  public LZW(Iterable<X> alphabet, X eof) {
    this(alphabet);
    this.EOF = eof;
  }

  /** Returns the number of entries in the r-dictionary which
    * start with a given prefix, by brute-force counting. */
  protected int prefixCount1(Vector<X> prefix) {
    int count = 0;
    for (Vector<X> w : rdict.keySet()) {
      if (prefix.size() <= w.size()) {
        List<X> pre = w.subList(0,prefix.size());
        if (prefix.equals(pre)) {
          count++;
        }
      }
    }
    return count;
  }
  
  /** Returns the number of entries in the r-dictionary which
    * start with a given prefix, by smart lookup. */
  protected int prefixCount(Vector<X> prefix) {
    if (prefixcount != null) {
      // fast, with lookup
      Integer num = prefixcount.get(prefix);
      return (num != null) ? num : 0;
    } else {
      // slow, with manual counting
      return prefixCount1(prefix);
    }
  }
  
  /** Returns the number of entries in the r-dictionary that
    * would start with x if buf:x were in the dictionary. */
  protected int prefixCountX1(Vector<X> buf, X x) {
    // an awful, inefficient method
    Vector<X> bufx = new Vector<X>(buf.size()+1);
    bufx.addAll(buf); bufx.add(x);
    Vector<X> vx = new Vector<X>(1); vx.add(x);
    if (rdict.containsKey(bufx)) {
      int nx = prefixCount(vx); // we can use the fast count
      return nx;
    } else {
      /*
      rdict.put(bufx,words);
      words++;
      // we must use the slow method for counting,
      // because the prefixcount cache isn't up to date.
      int nx = prefixCount1(vx);
      // FIXME: this can be done in a faster + smarter way
      rdict.remove(bufx);
      words--;
      */
      int nx = prefixCount(vx);
      if (bufx.get(0).equals(x)) {
        return nx+1;
      } else {
        return nx;
      }
    }
  }
  
  /** Returns the number of entries in the r-dictionary that
    * would start with x if buf:x were in the dictionary. */
  protected int prefixCountX(Vector<X> buf, X x) {
    // FIXME: we should do something smarter here
    return prefixCountX1(buf,x);
  }

  /** Inserts a word into rdict, the read dictionary. */
  protected void insertR(Vector<X> newentry) {
    if (rdict.containsKey(newentry)) {
      System.err.println("Warning: ALREADY IN DICT: "+newentry);
    }
    rdict.put(newentry, words);
    words++;
    // also update prefix counts
    if (prefixcount != null) {
      for (int k=1; k<=newentry.size(); k++) {
        List<X> pre = newentry.subList(0,k);
        Integer pcount = prefixcount.get(pre);
        if (pcount == null) {
          prefixcount.put(pre,1);
        } else {
          prefixcount.put(pre,pcount+1);
        }
      }
    }
  }
  
 
  /** Builds (or rebuilds) the hashmap of prefix counts. */
  protected void rebuildPrefixCounts() {
    System.err.println("Rebuilding prefix counts...");
    prefixcount = new HashMap<List<X>,Integer>();
    for (Vector<X> w : rdict.keySet()) {
      for (int k=1; k<w.size()+1; k++) {
        List<X> pre = w.subList(0,k);
        Integer pcount = prefixcount.get(pre);
        if (pcount == null) {
          prefixcount.put(pre,1);
        } else {
          prefixcount.put(pre,pcount+1);
        }
      }
    }
  }
  

  /** Returns an identification of this LZW compressor. */
  public String toString() {
    return "LZW(|alphabet|="+size+", |dict|="+words+")";
  }


  /** A simple incremental hash function. */
  public static int colhash(Iterable<Integer> col, int init) {
    if (col == null) {
      return 0;
    } else {
      int hash = init;
      for (int x : col) {
        hash = 31 * hash + x;
      }
      return hash;
    }
  }
  
  /** A simple incremental hash function. */
  public static int colhash(int x, int init) {
    return 31 * init + x;
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
    /* Case 1: buffer is empty: simply append x.
     * Case 2: buffer+x is in dictionary: append x.
     * Case 3: buffer+x is NOT in dictionary:
     *            output index(buffer),
     *            add "buffer+x" to dictionary,
     *            and set buffer to x.
     */
    Vector<X> tmpbuf = new Vector<X>(rbuf);
    if (tmpbuf.size() == 0) {
      return;
    } else {
      // invariant: rbuf is contained in current dictionary
      tmpbuf.add(x);
      // check if new rbuf is still contained in dictionary
      if (rdict.containsKey(tmpbuf)) {
        return;
      } else {
        /* current buffer is no longer in the dictionary:
         * so let's write the index of the currently longest
         * dictionary word, and store a new word. */
        u.encode(rdict.get(rbuf),ec);  // encode dictionary index
      }
    }
    if (EOF.equals(x)) {
      // flush symbols still in the buffer
      learn(x);
      flush(ec);
    }
  }

  boolean newword = false;
  public X decode(Decoder dc) {
    decoding = true;

    if (wbuf.size() == 0) {
      newword = true;
      int k = u.decode(dc);         // decode dictionary index
      wbuf = new Vector<X>(wdict.get(k));  // copy word from dictionary

      if (k == last) {
        wbuf.add(wbuf.get(0)); // wbuf is only partially complete, patch it up
      }
    }

    X x = wbuf.get(0);
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

  public double logMass(X x) {
    return Math.log(mass(x));
  }
  
  public double mass(X x) {
    if (prefixcount == null) {
      // enable operational speed-up for extra memory
      rebuildPrefixCounts();
    }
    Vector<X> vx = new Vector<X>(1);
    vx.add(x);
    /* what is the probability that the next symbol is x? */
    if (rbuf.size() == 0) {
      /* if nothing is in the buffer, the probability is proportional
         to the number of words starting with x, weighted appropriately
         by the word distribution u. */
      int num = prefixCount(vx);
      //System.err.println("!"+x+": "+num+" / "+words);
      // TODO: warning -- assumes that u is uniform:
      return (double) num / (double) words;
    } else {
      /* if something is in the buffer rbuf, then the probability is
       * proportional to the number of words starting with "rbuf:x",
       * dividing out the number of words starting with "rbuf"
       * (but excluding THOSE words that are ruled out).
       * If no dictionary entry starts with "rbuf:x", then "rbuf"
       * was the last word, "rbuf:x" is the new word, and "x"
       * remains in the buffer.
       * 
       * More information on "words that are ruled out":
       * Suppose we want to compute the probability of the symbol "X"
       * in the string "aaX", if all information about "aa" has
       * already been conveyed.
       * Before seeing X, the receiver cannot know which dictionary
       * entry was used for the second a: there are two possibilities.
       * This information is part of X and contributes 1 bit.
       * If the choice of X means that a new word is used,
       * then X also contributes information about the set of
       * words that it could start:
       * One might THINK that this probability equals the number of
       * entries starting with X divided by the total number
       * of entries: however, the probability is slightly higher,
       * as some entries can be ruled out using existing
       * knowledge (the preceding string "aa").  In particular,
       * X cannot be any symbol that the matching set of words in
       * the dictionary would have predicted (in which case a new
       * word wouldn't have been started).
       * For example, if "az" was in the dictionary, then LZW cannot
       * possibly have chosen a unigram entry for "a" and "z" if the
       * bigram "az" was available.  Therefore, knowing that "az"
       * was in the dictionary rules out the unigram "z".
       *
       * HOWEVER, the actual compression algorithm is deficient in
       * the same way: it uses a uniform distribution to encode
       * each dictionary entry without excluding those that are
       * ruled out in the more. So the current "broken" logp
       * calculation matches what the algorithm does in reality.
       * The probabilities don't sum to one, and that information
       * leak applies to the real algorithm as well.
       * Both should be fixed, and perhaps called LZW+. :) */
      //Vector<X> rbufx = new Vector<X>(rbuf.size()+1);
      Vector<X> rbufx = new Vector<X>();
      rbufx.addAll(rbuf);
      rbufx.add(x);
      int rbufcount  = prefixCount(rbuf);
      int rbufxcount = prefixCount(rbufx);
      //System.err.println("!  rbuf ="+rbuf+", rbufcount = "+rbufcount);
      //System.err.println("!  rbufx="+rbufx+", rbufxcount = "+rbufxcount);
      if (rbufxcount > 0) { // contained in dictionary: marginalise
        //System.err.println("! "+rbufxcount+" / "+rbufcount);
        // TODO: warning -- assumes that u is uniform:
        return (double) rbufxcount / (double) rbufcount;
      } else {
        // Count how many words would start with x if rbufx were in
        // the dictionary.
        int nx = prefixCountX(rbuf,x);
        double mass = (double) nx / ((double) (words+1) * (double) rbufcount);
        //System.err.println("! "+nx+" / ("+(words+1)+"*"+rbufcount+")");
        return mass;
      }
    }
    // FIXME: handle EOF specially?
  }

  /** A pointer to the newest dictionary entry, if known. */
  Vector<X> newentry = null;
  
  public void learn(X x) {
    if (decoding) {
      if (newword) {
        newword = false;

        // complete last dictionary entry
        if (last != -1) {
          Vector<X> obuf = new Vector<X>(wdict.get(last));
          obuf.add(x);               // add missing last symbol: x
          wdict.put(last,obuf);      // write completed word back to dict
        }

        // add partial new word to dictionary
        Vector<X> nbuf = new Vector<X>(wbuf);
        nbuf.add(0, x);
        wdict.put(words,nbuf);      // write incomplete word to dict
        last = words;
        words++;
        u.expand(1);
      }
    } else {
      /* Case 1: buffer is empty: simply append x.
       * Case 2: buffer+x is in dictionary: append x.
       * Case 3: buffer+x is NOT in dictionary:
       *            output index(buffer),
       *            add "buffer+x" to dictionary,
       *            and set buffer to x.
       */
      if (rbuf.size() == 0) {
        // CASE 1: buffer is empty
        rbuf.add(x);
      } else {
        // invariant: rbuf is contained in current dictionary
        rbuf.add(x);
        // check if new rbuf is still contained in dictionary
        if (rdict.containsKey(rbuf)) {
          newentry = null;
          return;
        } else {
          /* store new word in dictionary */
          newentry = new Vector<X>(rbuf);
          //rdict.put(newentry, words);
          //words++;
          insertR(newentry);
          u.expand(1);
          rbuf.clear();
          rbuf.add(x);   // buffer now contains the single symbol x
        }
      }
    }
  }

  public X sample(Random rnd) {
    DiscreteLookup<X> d = new DiscreteLookup<X>(this,alphabet);
    X x = d.sample(rnd);
    learn(x);
    return x;
  }

  public String getStateInfo() {
    return "w="+words+", "+newentry;
  }

  public static void main(String[] args) throws Exception {
    int k = 0;
    String ifnm = null;
    String ofnm = null;
    int mode = 0; // 0=compress, 1=decompress
    // parse options
    while (k < args.length) {
      if (args[k].equals("-h") || args[k].equals("--help")) {
        System.out.println("Usage: java LZW [options] infile");
        System.out.println("Compresses a file or stdin with a LZW-like algorithm.");
        System.out.println("Options:");
        System.out.println("\t-h, --help   Prints a helpful message");
        System.out.println("\t-d           Decompress (rather than compress)");
        System.out.println("\t-o FNM       Specifies an output filename, or '-' for stdout");
        System.out.println("If the input filename equals '-', stdin is used.");
        return;
      } else
      if (args[k].equals("-o")) {
        k++;
        ofnm = args[k];
      } else
      if (args[k].equals("-d")) {
        mode = 1;
      } else
      if (args[k].equals("-")) {
        ifnm = "-";
      } else
      if (args[k].startsWith("-")) {
        System.err.println("LZW: unknown option '"+args[k]+"'.");
        return;
      } else {
        if (ifnm == null) {
          ifnm = args[k];
        } else {
          System.err.println("LZW: only one input file is supported.");
        }
      }
      k++;
    }
    // adjust settings
    if (ifnm == null) {
      System.err.println("LZW: no input file specified. Try '--help'.");
      return;
    } else
    if (ofnm == null) {
      if (ifnm.equals("-")) {
        ofnm = "-";
      } else {
        if (mode == 0) {
          ofnm = ifnm+".lzw";
        } else {
          if (ifnm.endsWith(".lzw")) {
            ofnm = ifnm.substring(0,ifnm.length()-4);
          } else {
              System.err.println("LZW: set an outfile filename with '-o FNM'.");
              return;
          }
        }
      }
    }
    if (ifnm.equals("-")) { ifnm = ""; }
    if (ofnm.equals("-")) { ofnm = ""; }
    // time for action!
    LZW<Integer> lzw =
               new LZW<Integer>(ByteCompressor.base, ByteCompressor.eof);
    ByteCompressor bc = new ByteCompressor(lzw);
    if (mode == 0) { // compress...
      bc.compress(ifnm, ofnm, new Arith());
      /*
      System.err.println("LZW R dictionary has "+lzw.words+" entries.");
      System.err.println("LZW R uniform dist: "+lzw.u);
      lzw.printRDict();
      */
    } else
    if (mode == 1) { // decompress...
      bc.decompress(ifnm, ofnm, new Arith());
      /*
      System.err.println("LZW W dictionary has "+lzw.words+" entries.");
      System.err.println("LZW W uniform dist: "+lzw.u);
      lzw.printWDict();
      */
    } else {
      System.err.println("LZW: unknown internal mode of operation.");
      return;
    }
  }

}
