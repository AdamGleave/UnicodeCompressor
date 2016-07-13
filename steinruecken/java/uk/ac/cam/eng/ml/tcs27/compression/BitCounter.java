/* $Id$ */
package uk.ac.cam.eng.ml.tcs27.compression;

import java.io.IOException;

/** A class which counts the number of bits in a bit stream. */
public class BitCounter implements BitWriter, BitReader {
 
  protected BitWriter bw;
  protected BitReader br;

  /** Number of bits written. */
  protected long wcount = 0L;

  /** Number of bits read. */
  protected long rcount = 0L;

  /** Writes a bit. */
  public void writeBit(byte b) throws IOException {
    ++wcount;
    if (bw != null) { bw.writeBit(b); }
  }

  /** Reads a bit. */
  public byte readBit() throws IOException {
    if (br != null) {
      ++rcount;
      return br.readBit();
    } else {
      throw new IOException();
    }
  }

  /** Returns the number of bits written. */
  public long bitsWritten() {
    return wcount;
  }
  
  /** Returns the number of bits read. */
  public long bitsRead() {
    return rcount;
  }

  public void close() throws IOException {
    if (bw != null) { bw.close(); }
    if (br != null) { br.close(); }
  }

  public boolean informative() {
    if (br != null) {
      return br.informative();
    } else {
      return false;
    }
  }

  /** Constructs a new BitCounter.
    * The initial bit count is 0. */
  public BitCounter() {
  }

  /** Constructs a new BitCounter wrapping around a given BitWriter.
    * The initial bit count is 0. */
  public BitCounter(BitWriter bw) {
    this.bw = bw;
  }
  
  /** Constructs a new BitCounter wrapping around a given BitReader.
    * The initial bit count is 0. */
  public BitCounter(BitReader br) {
    this.br = br;
  }

}
