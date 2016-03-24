/* Automated copy from build process */
/* $Id: BitReaderSequence.java,v 1.5 2016/03/02 02:22:51 chris Exp $ */

import java.io.IOException;
import java.io.EOFException;
import java.util.Queue;
import java.util.LinkedList;

/** A BitReader reading bits from several BitReaders in sequence.
  * As soon as a BitReader throws an IOException, the
  * next BitReader in the sequence is used. */
public class BitReaderSequence implements BitReader {
  
  /** Queue of BitReaders, to be read from in sequence. */
  private Queue<BitReader> queue = null;

  /** Currently active BitReader (removed from queue). */
  private BitReader current = null;

  /** Constructs a new (empty) BitReaderSequence. */
  public BitReaderSequence() {
    queue = new LinkedList<BitReader>();
  }

  /** Constructs a BitReaderSequence containing all supplied
    * BitReaders. */
  public BitReaderSequence(Iterable<BitReader> bitreaders) {
    queue = new LinkedList<BitReader>();
    for (BitReader br : bitreaders) {
      queue.add(br);
    }
  }

  /** Constructs a BitReaderSequence containing two BitReaders. */
  public BitReaderSequence(BitReader br1, BitReader br2) {
    queue = new LinkedList<BitReader>();
    queue.add(br1);
    queue.add(br2);
  }

  /** Appends a BitReader to the queue of BitReaders. */
  public void append(BitReader br) {
    queue.add(br);
  }

  /** A static method which produces a new BitReader by
    * padding the supplied BitReader with an infinite sequence
    * of zero-bits. */
  public static BitReaderSequence appendZeros(BitReader br) {
    return new BitReaderSequence(br,new ConstantBitReader((byte) 0));
  }
  
  /** A static method which produces a new BitReader by
    * padding the supplied BitReader with an infinite sequence
    * of one-bits. */
  public static BitReaderSequence appendOnes(BitReader br) {
    return new BitReaderSequence(br,new ConstantBitReader((byte) 1));
  }

  public byte readBit() throws IOException {
    if (current != null) {
      try {
        return current.readBit();
      }
      catch (EOFException e) {
        try { current.close(); }
        catch (IOException f) { }
      }
      catch (IOException e) {
        try { current.close(); }
        catch (IOException f) { }
      }
    }
    if (!queue.isEmpty()) {
      current = queue.remove();
      return this.readBit();
    } else {
      throw new EOFException();
    }
  }

  public boolean informative() {
    return !queue.isEmpty();
  }

  public void close() {
    // FIXME: should we do anything here?
  }

}
