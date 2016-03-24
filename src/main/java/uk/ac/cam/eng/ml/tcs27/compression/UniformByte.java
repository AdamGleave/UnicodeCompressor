/* Automated copy from build process */
/* $Id: UniformByte.java,v 1.1 2013/06/30 14:13:28 chris Exp $ */

import java.util.Random;
import java.util.Iterator;

/** Uniform distribution over all byte values. */
public class UniformByte extends SimpleMass<Byte> implements Iterable<Byte> {

  static final double mass = 1.0/256.0;
  static final double logm = -Math.log(256);

  public double mass(Byte b) { return mass; }
  public double logMass(Byte b) { return logm; }
  public boolean isFinite() {  return true; }
  public boolean isIterable() { return true; }
  public double entropy() { return -logm; }
  public void learn(Byte b) { }

  public Byte sample(Random rnd) {
    byte[] bb = new byte[1];
    rnd.nextBytes(bb);
    return bb[0];
  }

  public void encode(Byte b, Encoder ec) {
    long pos = (long) b & 0xFF; // convert to unsigned 0..255
    ec.storeRegion(pos,pos+1,256);
  }
  
  public Byte decode(Decoder dc) {
    long pos = dc.getTarget(256);
    byte b = (byte) pos;
    dc.loadRegion(pos,pos+1,256);
    return b;
  }

  public Iterator<Byte> iterator() {
    return new Iterator<Byte>() {
      int next = 0;
      public boolean hasNext() {
        return (next < 256);
      }
      public Byte next() {
        if (next < 256) {
          Byte b = (byte) next;
          next++;
          return b;
        } else {
          return null;
        }
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }


}
