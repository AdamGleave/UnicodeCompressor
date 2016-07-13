package uk.ac.cam.eng.ml.tcs27.compression;

import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Iterator;

public class FixedSeq<X> extends SimpleMass<X> {

  private Iterator<X> seq = null; 

  /** Next symbol in the sequence. */
  X next = null;

  /** Indicates if the sequence is still active. */
  boolean active = true;

  /** Total number of symbols seen so far. */
  long pos = -1L;
  
  /** Indicates if the current symbol is up to date. */
  protected boolean uptodate = false;

  protected String label = "<unknown>";

  public String toString() {
    return "FixedSeq("+label+")";
  }

  /** Constructs a FixedSeq from an Iterable. */
  public FixedSeq(Iterable<X> sequence) {
    this.seq = sequence.iterator();
  }
  
  /** Constructs a FixedSeq from an Iterable. */
  public FixedSeq(Iterable<X> sequence, String label) {
    this.seq = sequence.iterator();
    this.label = label;
  }
  
  /** Constructs a FixedSeq from an Iterator. */
  public FixedSeq(Iterator<X> iterator) {
    this.seq = iterator;
  }
  
  /** Constructs a FixedSeq from an Iterator. */
  public FixedSeq(Iterator<X> iterator, String label) {
    this.seq = iterator;
    this.label = label;
  }

  /** Constructs a char-valued FixedSeq for a given file. */
  public static FixedSeq<Character> charSeqFromFile(String fnm) {
    return new FixedSeq<Character>(IOTools.charSequenceFromFile(fnm),
                                   "char,'"+fnm+"'");
  }
  
  /** Constructs a byte-valued FixedSeq for a given file. */
  public static FixedSeq<Byte> byteSeqFromFile(String fnm) {
    return new FixedSeq<Byte>(IOTools.byteSequenceFromFile(fnm),
                              "byte,'"+fnm+"'");
  }

  /** Reads the next symbol from the sequence into the cache. */
  protected void fetch() {
    active = seq.hasNext();
    if (active) {
      next = seq.next();
      pos++;
    } else {
      next = null;
    }
    uptodate = true;
  }

  /** Returns the next symbol in the sequence.
    * This method is purely deterministic and no randomness is used. */
  public X sample(Random rnd) {
    if (!uptodate) { fetch(); }
    if (active) {
      uptodate = false;
      return next;
    } else {
      throw new NoSuchElementException();
    }
  }

  public void learn(X x) {
    if (active) {
      uptodate = false;
      if (!next.equals(x)) {
        throw new ZeroMassException();
      }
    } else {
      throw new NoSuchElementException();
    }
  }

  /** Returns the probability mass of the next symbol.
    * @return 1.0 (if the symbol is correct) or 0.0 (otherwise). */
  public double mass(X x) {
    if (!uptodate) { fetch(); }
    if (active) {
      if (next.equals(x)) {
        return 1.0;
      } else {
        return 0.0;
      }
    } else {
      throw new NoSuchElementException();
    }
  }
  
  /** Returns the probability mass of the next symbol.
    * @return 0.0 (if the symbol is correct) or -∞ (otherwise). */
  public double logMass(X x) {
    if (!uptodate) { fetch(); }
    if (active) {
      if (next.equals(x)) {
        return 0.0;
      } else {
        return Double.NEGATIVE_INFINITY;
      }
    } else {
      throw new NoSuchElementException();
    }
  }


}
