/* Automated copy from build process */
/* $Id: RegionTracer.java,v 1.6 2014/10/07 01:18:16 chris Exp $ */

import java.util.Queue;
import java.util.LinkedList;

/** A debugging tool for instances of Codable.
  * This class provides a coupled Encoder and Decoder, checking that
  * the calls made to
  * {@code storeRegion}, {@code loadRegion} and {@code getTarget}
  * match up.  This can be used instead of an arithmetic coder, to
  * aid debugging the coding mechanism of Codable models.
  * @see Codable */
public class RegionTracer implements Encoder, Decoder {

  Queue<Triple<Long,Long,Long>> queue = 
                           new LinkedList<Triple<Long,Long,Long>>();
  long encoded = 0;
  long decoded = 0;

  public void storeRegion(long l, long h, long t) {
    if (l==h) {
      System.err.println("Warning: attempt to encode zero-mass event.");
      System.err.println("Warning: storeRegion("+l+","+h+","+t+")");
      throw new ZeroMassException();
    }
    queue.add(Triple.of(l,h,t));
    encoded++;
  }
  
  public void storeRegion(long l, long h) {
    if (l==h) {
      System.err.println("Warning: attempt to encode zero-mass event.");
      System.err.println("Warning: storeRegion("+l+","+h+")");
      throw new ZeroMassException();
    }
    long r = getRange();
    queue.add(Triple.of(l,h,r));
    encoded++;
  }

  public void loadRegion(long l, long h, long t) {
    Triple<Long,Long,Long> trip = queue.remove();
    decoded++;
    if (trip.get0() != l || trip.get1() != h || trip.get2() != t) {
      // if the region does not match up, throw an exception
      throw new RuntimeException("Region trace diverged at step "+decoded
             +": expected "+trip+" but tried loading <"+l+","+h+","+t+">.");
    }
  }
  
  public void loadRegion(long l, long h) {
    Triple<Long,Long,Long> trip = queue.remove();
    decoded++;
    long r = getRange();
    if (trip.get0() != l || trip.get1() != h || trip.get2() != r) {
      // if the region does not match up, throw an exception
      String reg = "<"+trip.get0()+","+trip.get1()+"> with R="+trip.get2();
      throw new RuntimeException("Region trace diverged at step "+decoded
             +": expected "+trip+" but tried loading <"+l+","+h+"> with R="+r+">.");
    }
  }
  
  public long getTarget() {
    return getTarget(getRange());
  }

  public long getTarget(long tt) {
    Triple<Long,Long,Long> trip = queue.peek();
    if (trip != null) {
      long l = trip.get0();
      long h = trip.get1();
      long t = trip.get2();
      if (t != tt) {
        System.err.println("Warning: decoder asked for unexpected target"
                  +" range ("+tt+", but expected "+t+").");
      }
      return l+(h-l)/2;
    } else {
      System.err.println("Warning: decoder asked for target at step "+decoded
                        +", but queue is empty.");
      throw new IllegalStateException("queue is empty -- no target");
    }
  }
  
  public long getRange() {
    // FIXME: maybe give it something closer to the maximum range?
    return 01L << 36;
  }

}
